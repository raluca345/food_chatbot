package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generated food image")
public class ImageDto {

    @Schema(
            description = "Unique image identifier",
            example = "1"
    )
    private long id;

    @Schema(
            description = "URL of the generated image",
            example = "https://cdn.example.com/images/pasta-carbonara.jpg"
    )
    private String url;

    @Schema(
            description = "URL of the image's thumbnail",
            example = "https://cdn.example.com/images/pasta-carbonara_thumbnail.webp"
    )
    private String thumbnailUrl;

    @Schema(
            description = "The original image's file name",
            example = "pasta_carbonara.jpg"
    )
    private String filename;

    @Schema(
            description = "The name of the thumbnail of the image",
            example = "past_carbonara_thumbnail.webp"
    )
    private String thumbnailFilename;

    @Schema(
            description = "Timestamp when the image was created",
            example = "2026-05-07T10:30:00"
    )
    private LocalDateTime createdAt;
}

