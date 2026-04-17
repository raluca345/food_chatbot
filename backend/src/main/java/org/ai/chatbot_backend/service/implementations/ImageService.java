package org.ai.chatbot_backend.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.FoodImageRequest;
import org.ai.chatbot_backend.dto.ImageContent;
import org.ai.chatbot_backend.dto.ImageDto;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.Image;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.ImageRepository;
import org.ai.chatbot_backend.service.interfaces.IImageService;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClient;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService implements IImageService {

    @Value("${cloudflare.r2.bucket}")
    @Getter
    private String bucket;
    @Value("${image.mock:false}")
    private boolean mockEnabled;
    @Value("${azure.mai.endpoint}")
    private String maiEndpoint;
    @Value("${azure.mai.api-key}")
    private String maiApiKey;

    private final UserService userService;
    private final S3Client r2Client;
    @Getter
    private final R2Service r2Service;
    private final RestClient restClient;

    private final ImageRepository imageRepository;

    @Override
    public String generateFoodImageFromParams(FoodImageRequest request) {
        if (mockEnabled) {
            return "https://picsum.photos/512";
        }

        if (request == null) {
            throw new InappropriateRequestRefusalException("Sorry, the request is invalid");
        }

        String name = request.getName();
        String course = request.getCourse();
        String ingredients = request.getIngredients();
        String dishType = request.getDishType();
        String style = request.getStyle();
        String normalizedSize = getNormalizedSize(request, style);

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
        String[] parts = normalizedSize.split("x");
        if (parts.length != 2) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }
        try {
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }

        try {
            String promptText = String.valueOf(prompt);
            return callMaiImageApi(promptText, width, height);
        } catch (Exception e) {
            log.error("Azure MAI image generation error: {}", e.getMessage(), e);
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }

    private String callMaiImageApi(String prompt, int width, int height) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "prompt", prompt,
                "width", width,
                "height", height,
                "n", 1,
                "model", "MAI-Image-2e"
        );

        // turn into json string to ensure content-length header is set properly
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(maiEndpoint)
                .header("api-key", maiApiKey)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> data = (List<Map<String, String>>) response.get("data");
        if (data == null || data.isEmpty()) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }

        Map<String, String> result = data.getFirst();

        if (result.containsKey("b64_json")) {
            String base64 = result.get("b64_json");
            // return as data uri, don't persist to R2 yet, that happens in persistImageForUser
            return "data:image/png;base64," + base64;
        }

        if (result.containsKey("url")) {
            return result.get("url");
        }

        throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
    }

    private static @NonNull String getNormalizedSize(FoodImageRequest request, String style) {
        String size = request.getSize();

        if (style == null || (!style.equalsIgnoreCase("vivid") && !style.equalsIgnoreCase("natural"))) {
            throw new InappropriateRequestRefusalException("Sorry, the picked style is invalid");
        }

        if (size == null || size.isBlank()) {
            size = "1024x1024";
        }
        String normalizedSize = size.trim().toLowerCase();
        if (!normalizedSize.equals("1024x1024")
                && !normalizedSize.equals("1024x1792")
                && !normalizedSize.equals("1792x1024")) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }
        return normalizedSize;
    }

    public ImageDto persistImageForUser(String tempUrl, Long userId) throws Exception {
        Path tempFile = Files.createTempFile("img-", ".png");

        // handle data uri (from image generation)
        if (tempUrl.startsWith("data:image")) {
            String base64 = tempUrl.substring(tempUrl.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            Files.write(tempFile, imageBytes);
        } else {
            // handle regular url
            try (InputStream in = new URI(tempUrl).toURL().openStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
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
        Image savedImage = imageRepository.save(image);

        Files.deleteIfExists(tempFile);

        return new ImageDto(savedImage.getId(), signedUrl, savedImage.getFilename(), savedImage.getCreatedAt());
    }

    @Override
    public PageResult<ImageDto> getImages(User user, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 18;

        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Image> p = imageRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<ImageDto> items = p.stream().map(img -> {
            String signedUrl = r2Service.generateSignedUrl(img.getFilename());
            return new ImageDto(img.getId(), signedUrl, img.getFilename(), img.getCreatedAt());
        }).toList();

        return new PageResult<>(List.copyOf(items), p.getTotalElements());
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
        } catch (RuntimeException e) {
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

