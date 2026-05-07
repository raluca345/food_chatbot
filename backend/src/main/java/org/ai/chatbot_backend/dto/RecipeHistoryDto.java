package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recipe history entry")
public class RecipeHistoryDto {
    @Schema(
            description = "Unique identifier for the recipe history entry",
            example = "1"
    )
    private Long id;

    @Schema(
            description = "Recipe title",
            example = "Spaghetti Carbonara"
    )
    private String title;

    @Schema(
            description = "Recipe content in markdown format",
            example = "# Spaghetti Carbonara\n\n## Ingredients..."
    )
    private String content;

    @Schema(
            description = "Associated file ID for downloaded recipe",
            example = "42"
    )
    private Long fileId;

    @Schema(
            description = "Timestamp when the recipe was created",
            example = "2026-05-07T10:30:00"
    )
    private LocalDateTime createdAt;
}

