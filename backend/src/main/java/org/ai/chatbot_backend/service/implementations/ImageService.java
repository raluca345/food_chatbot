package org.ai.chatbot_backend.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService implements IImageService {
    private static final int MIN_IMAGE_DIMENSION = 768;
    private static final int MAX_TOTAL_PIXELS = 1_048_576;
    private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+)x(\\d+)$");
    private static final String DEFAULT_REFUSAL_MESSAGE = "Sorry, I can only generate images of food.";

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

    @Value("classpath:non_food_keywords.txt")
    private Resource keywordsFile;
    private Set<String> nonFoodKeywords = Set.of();

    @PostConstruct
    public void loadKeywords() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(keywordsFile.getInputStream()))) {
            Set<String> loadedKeywords = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            if (loadedKeywords.isEmpty()) {
                throw new IllegalStateException("non_food_keywords.txt is empty");
            }

            nonFoodKeywords = loadedKeywords;
        }
    }

    private void validateNoNonFoodKeywords(FoodImageRequest request) {
        validateField(request.getName());
        validateField(request.getCourse());
        validateField(request.getIngredients());
        validateField(request.getDishType());
    }

    private void validateField(String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        String lower = value.toLowerCase(Locale.ROOT);

        List<String> words = Arrays.stream(lower.split("\\W+"))
                .filter(s -> !s.isBlank())
                .toList();

        for (String keyword : nonFoodKeywords) {
            if (words.contains(keyword)) {
                log.warn("Blocked keyword '{}' detected in value '{}'", keyword, value);
                throw new InappropriateRequestRefusalException(DEFAULT_REFUSAL_MESSAGE);
            }
        }
    }

    @Override
    public String generateFoodImageFromParams(FoodImageRequest request) {
        if (mockEnabled) {
            return "https://picsum.photos/512";
        }

        if (request == null) {
            throw new InappropriateRequestRefusalException("Sorry, the request is invalid");
        }

        validateNoNonFoodKeywords(request);

        String style = request.getStyle();
        String normalizedSize = getNormalizedSize(request, style);

        String promptText = buildPrompt(request, style);

        String[] parts = normalizedSize.split("x");

        int width;
        int height;

        try {
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }

        try {
            log.info("Final MAI prompt: {}", promptText);
            return callMaiImageApi(promptText, width, height);
        } catch (InappropriateRequestRefusalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Azure MAI image generation error: {}", e.getMessage(), e);
            throw new InappropriateRequestRefusalException(DEFAULT_REFUSAL_MESSAGE);
        }
    }

    private String buildPrompt(FoodImageRequest request, String style) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Create a professional, appetizing ");

        if ("vivid".equalsIgnoreCase(style)) {
            prompt.append("vibrant, colorful ");
        } else if ("natural".equalsIgnoreCase(style)) {
            prompt.append("realistic, natural-looking ");
        }

        prompt.append("food image.");

        if (request.getName() != null && !request.getName().isBlank()) {
            prompt.append(" It has the name: ")
                    .append(request.getName())
                    .append(".");
        }

        if (request.getCourse() != null && !request.getCourse().isBlank()) {
            prompt.append(" It is a ")
                    .append(request.getCourse())
                    .append(".");
        }

        if (request.getIngredients() != null && !request.getIngredients().isBlank()) {
            prompt.append(" The ingredients are ")
                    .append(request.getIngredients())
                    .append(".");
        }

        if (request.getDishType() != null && !request.getDishType().isBlank()) {
            prompt.append(" The type of dish is ")
                    .append(request.getDishType())
                    .append(".");
        }

        return prompt.toString();
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
        
        log.debug("Sending image generation request - Prompt: {}, Dimensions: {}x{}", prompt, width, height);

        Map<String, Object> response;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.post()
                    .uri(maiEndpoint)
                    .header("api-key", maiApiKey)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .retrieve()
                    .body(Map.class);
            response = body;
        } catch (HttpClientErrorException e) {
            log.error("Azure MAI API error - Status: {}, Message: {}, Response: {}", 
                    e.getStatusCode(), e.getMessage(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 400) {
                log.error("MAI 400 RESPONSE BODY: {}", e.getResponseBodyAsString());

                throw new InappropriateRequestRefusalException(
                        "MAI API returned 400: " + e.getResponseBodyAsString()
                );
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling Azure MAI API: {}", e.getMessage(), e);
            throw e;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> data = (List<Map<String, String>>) response.get("data");
        if (data == null || data.isEmpty()) {
            throw new InappropriateRequestRefusalException(DEFAULT_REFUSAL_MESSAGE);
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

        throw new InappropriateRequestRefusalException(DEFAULT_REFUSAL_MESSAGE);
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
        Matcher matcher = SIZE_PATTERN.matcher(normalizedSize);
        if (!matcher.matches()) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }

        int width;
        int height;
        try {
            width = Integer.parseInt(matcher.group(1));
            height = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }

        long totalPixels = (long) width * height;
        if (width < MIN_IMAGE_DIMENSION || height < MIN_IMAGE_DIMENSION || totalPixels > MAX_TOTAL_PIXELS) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }

        return normalizedSize;
    }

    private Path createThumbnail(Path fileName) throws IOException {
        Path thumbnailFile = Files.createTempFile("thumb-", ".jpg");
        Thumbnails.of(fileName.toFile())
                .size(300, 300)
                .outputFormat("jpg")
                .toFile(thumbnailFile.toFile());

        return thumbnailFile;
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

        Path thumbnailFile = createThumbnail(tempFile);
        String thumbnailFilename = "users/" + userId + "/thumbnails/" + UUID.randomUUID() + ".jpg";
        r2Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(thumbnailFilename)
                        .contentType("image/jpeg")
                        .build(),
                thumbnailFile
        );

        String signedUrl = r2Service.generateSignedUrl(filename);
        String signedThumbnailUrl = r2Service.generateSignedUrl(thumbnailFilename);

        User user = userService.findById(userId);

        Image image = Image.builder()
                .user(user)
                .filename(filename)
                .thumbnailFilename(thumbnailFilename)
                .createdAt(LocalDateTime.now())
                .build();
        Image savedImage = imageRepository.save(image);

        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(thumbnailFile);

        return new ImageDto(savedImage.getId(), signedUrl, signedThumbnailUrl, savedImage.getFilename(),
                savedImage.getThumbnailFilename(), savedImage.getCreatedAt());
    }

    @Override
    public PageResult<ImageDto> getImages(User user, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 18;

        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Image> p = imageRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<ImageDto> items = p.stream().map(img -> {
            String signedUrl = r2Service.generateSignedUrl(img.getFilename());
            String signedThumbnailUrl = img.getThumbnailFilename() == null
                    ? signedUrl
                    : r2Service.generateSignedUrl(img.getThumbnailFilename());
            return new ImageDto(img.getId(), signedUrl, signedThumbnailUrl, img.getFilename(),
                    img.getThumbnailFilename(), img.getCreatedAt());
        }).toList();

        return new PageResult<>(List.copyOf(items), p.getTotalElements());
    }

    @Override
    public void deleteByIdForUser(long imageId, User user) {
        Image image = imageRepository.findById(imageId).orElseThrow(() ->
                new ResourceNotFoundException("Image with id " + imageId + " not found"));

        if (image.getUser() == null || image.getUser().getId() != user.getId()) {
            throw new ResourceNotFoundException("Image not found");
        }

        String filename = image.getFilename();
        String thumbnailFilename = image.getThumbnailFilename();

        deleteObject(filename, "image");
        if (thumbnailFilename != null && !thumbnailFilename.isBlank()) {
            deleteObject(thumbnailFilename, "image thumbnail");
        }

        imageRepository.deleteById(imageId);
    }

    private void deleteObject(String key, String label) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            r2Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to delete " + label + " from storage: "
                    + e.awsErrorDetails().errorMessage(), e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to delete " + label + " from storage", e);
        }
    }

    @Override
    public Image getImageForUser(long imageId, User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image with id " + imageId + " not found"));

        if (image.getUser() == null || image.getUser().getId() != user.getId()) {
            throw new ResourceNotFoundException("Image not found");
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

