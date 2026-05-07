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
            description = "Original filename of the image",
            example = "pasta_carbonara.jpg"
    )
    private String filename;

    @Schema(
            description = "Timestamp when the image was created",
            example = "2026-05-07T10:30:00"
    )
    private LocalDateTime createdAt;
}

