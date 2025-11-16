package org.ai.chatbot_backend.service.implementations;

import com.azure.core.exception.HttpResponseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.ImageContent;
import org.ai.chatbot_backend.dto.ImageDto;
import org.ai.chatbot_backend.dto.ImagePageDto;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.Image;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.ImageRepository;
import org.ai.chatbot_backend.service.interfaces.IImageService;
import org.springframework.ai.azure.openai.AzureOpenAiImageModel;
import org.springframework.ai.azure.openai.AzureOpenAiImageOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService implements IImageService {

    @Value("${cloudflare.r2.bucket}")
    @Getter
    private String bucket;

    private final UserService userService;
    private final S3Client r2Client;
    @Getter
    private final R2Service r2Service;
    private final AzureOpenAiImageModel azureOpenAiImageModel;

    private final ImageRepository imageRepository;

    @Override
    public String generateFoodImageFromParams(String name, String course, String ingredients, String dishType,
                                              String style, String size) {

        String systemPrompt = "You are a helpful assistant that only generates images of food. Do not generate images" +
                " of anything else.";
        StringBuilder templateBuilder = new StringBuilder(systemPrompt +
                " I want an image of a dish with the name: {name}.");

        if (name == null || name.isBlank() || name.equals("null")) {
            name = "any";
        }

        if (course != null && !course.isBlank()) {
            templateBuilder.append(" It is a ").append("{course}").append(".");
        }
        if (ingredients != null && !ingredients.isBlank()) {
            templateBuilder.append(" The ingredients are ").append("{ingredients}").append(".");
        }
        if (dishType != null && !dishType.isBlank()) {
            templateBuilder.append(" The type of dish is ").append("{dishType}").append(".");
        }

        String template = templateBuilder.toString();
        PromptTemplate promptTemplate = new PromptTemplate(template);

        Map<String, Object> params = Map.of(
                "name", name,
                "course", course != null ? course : "",
                "ingredients", ingredients != null ? ingredients : "",
                "dishType", dishType != null ? dishType : ""
        );
        Prompt prompt = promptTemplate.create(params);

        int width, height;
        try {
            String[] parts = size.toLowerCase().split("x");
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }

        try {
            ImageResponse response = azureOpenAiImageModel.call(
                    new ImagePrompt(String.valueOf(prompt),
                            AzureOpenAiImageOptions.builder()
                                    .style(style)
                                    .width(width)
                                    .height(height)
                                    .build())
            );
            if (response.getResults().isEmpty()) {
                throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
            }
            return response.getResult().getOutput().getUrl();
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }

    public String persistImageForUser(String tempUrl, Long userId) throws Exception {

        Path tempFile = Files.createTempFile("img-", ".png");
        try (InputStream in = new URI(tempUrl).toURL().openStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        String filename = "users/" + userId + "/images/" + UUID.randomUUID() + ".png";

        r2Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(filename)
                        .contentType("image/png")
                        .build(),
                tempFile
        );

        String signedUrl = r2Service.generateSignedUrl(filename);

        User user = userService.findById(userId);

        Image image = Image.builder()
                .user(user)
                .filename(filename)
                .createdAt(LocalDateTime.now())
                .build();
        imageRepository.save(image);

        Files.deleteIfExists(tempFile);

        return signedUrl;
    }

    @Override
    public List<ImageDto> getAllImagesForUser(User user) {
        return imageRepository.findByUserId(user.getId())
                .stream()
                .map(img -> {
                    String signedUrl = r2Service.generateSignedUrl(img.getFilename());
                    return new ImageDto(img.getId(), signedUrl, img.getFilename(), img.getCreatedAt());
                })
                .toList();
    }

    @Override
    public ImagePageDto getImagesForUserPaged(User user, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 18;

        List<ImageDto> images = getAllImagesForUser(user);
        if (images == null || images.isEmpty()) {
            return new ImagePageDto(List.copyOf(List.of()), 0);
        }

        List<ImageDto> sorted = new ArrayList<>(images);
        sorted.sort(Comparator.comparing(ImageDto::getCreatedAt).reversed());

        int total = sorted.size();
        int start = (page - 1) * pageSize;
        if (start >= total) {
            return new ImagePageDto(List.copyOf(List.of()), total);
        }
        int end = Math.min(start + pageSize, total);
        List<ImageDto> pageItems = new ArrayList<>(sorted.subList(start, end));
        List<ImageDto> immutablePage = List.copyOf(pageItems);

        return new ImagePageDto(immutablePage, total);
    }

    @Override
    public void deleteById(long id) {
        if (imageRepository.existsById(id)) {
            imageRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Image with id " + id + " not found");
        }
    }

    @Override
    public void deleteByIdForUser(long imageId, User user) {
        Image image = imageRepository.findById(imageId).orElseThrow(() ->
                new ResourceNotFoundException("Image with id " + imageId + " not found"));

        if (image.getUser() == null || image.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("User does not have permission to delete this image");
        }

        String filename = image.getFilename();

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .build();
            r2Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to delete image from storage: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image from storage", e);
        }

        imageRepository.deleteById(imageId);
    }

    @Override
    public Image getImageForUser(long imageId, User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image with id " + imageId + " not found"));

        if (image.getUser() == null || image.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("User does not have permission to access this image");
        }

        return image;
    }

    @Override
    public ImageContent loadImageContentForUser(long imageId, User user) {
        Image image = getImageForUser(imageId, user);
        String key = image.getFilename();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try {
            ResponseInputStream<GetObjectResponse> s3Object = r2Client.getObject(getObjectRequest);
            Resource resource = new InputStreamResource(s3Object);

            String downloadName = key.substring(key.lastIndexOf('/') + 1);
            String contentType = "image/png";

            return new ImageContent(resource, downloadName, contentType);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to fetch image from storage", e);
        }
    }

}
