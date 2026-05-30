package org.ai.chatbot_backend.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        for (String keyword : nonFoodKeywords) {
            if (lower.contains(keyword)) {
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

        String name = request.getName();
        String course = request.getCourse();
        String ingredients = request.getIngredients();
        validateNoNonFoodKeywords(request);
        String dishType = request.getDishType();
        String style = request.getStyle();
        String normalizedSize = getNormalizedSize(request, style);

        StringBuilder templateBuilder = getSystemPrompt();

        if (name != null && !name.isBlank() && !name.equals("null")) {
            templateBuilder.append("It has the name: {name}.");
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
                "name", name != null ? name : "",
                "course", course != null ? course : "",
                "ingredients", ingredients != null ? ingredients : "",
                "dishType", dishType != null ? dishType : ""
        );
        Prompt prompt = promptTemplate.create(params);

        int width, height;
        String[] parts = normalizedSize.split("x");
        try {
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }

        try {
            String promptText = String.valueOf(prompt);
            return callMaiImageApi(promptText, width, height);
        } catch (InappropriateRequestRefusalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Azure MAI image generation error: {}", e.getMessage(), e);
            throw new InappropriateRequestRefusalException(DEFAULT_REFUSAL_MESSAGE);
        }
    }

    @NotNull
    private static StringBuilder getSystemPrompt() {
        String systemPrompt = """
                You are a food-only image generation assistant.

                STRICT RULES:
                - Generate only edible food or drink.
                - Refuse requests containing non-food objects or inedible items.
                - Never reinterpret non-food items as garnish, props, plating, art, or decoration.
                - Never include tools, hardware, chemicals, drugs, weapons, or other non-food objects.

                If any non-food item is requested, refuse the request and do not generate an image.
                """;
        return new StringBuilder(systemPrompt +
                " Create an image of a dish. ");
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
            if (e.getStatusCode().value() == 400) {
                throw new InappropriateRequestRefusalException(DEFAULT_REFUSAL_MESSAGE);
            }
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
            throw new ResourceNotFoundException("Image not found");
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

