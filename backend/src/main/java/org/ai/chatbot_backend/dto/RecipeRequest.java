package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RecipeRequest {
    @Schema(
            description = "Comma-separated list of ingredients",
            example = "chicken breast, tomatoes, garlic, basil, olive oil"
    )
    private String ingredients;

    @Schema(
            description = "Cuisine type",
            example = "mediterranean"
    )
    private String cuisine;

    @Schema(
            description = "Dietary restrictions or preferences",
            example = "high-protein, low-carb"
    )
    private String dietaryRestrictions;
}
