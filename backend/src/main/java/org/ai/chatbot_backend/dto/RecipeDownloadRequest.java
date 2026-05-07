package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to download a recipe for guest users")
public class RecipeDownloadRequest {
    @Schema(
            description = "Recipe content in markdown format",
            example = "# Spaghetti Carbonara\n\n## Ingredients\n- 400g spaghetti\n- 200g pancetta"
    )
    private String recipeMarkdown;
}
