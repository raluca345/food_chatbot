package org.ai.chatbot_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Generated recipe response from AI")
public class RecipeResponse {
    @Schema(
            description = "Recipe title",
            example = "Spaghetti Carbonara"
    )
    private String title;

    @JsonProperty("recipe_markdown")
    @Schema(
            description = "Recipe content in markdown format",
            example = "# Spaghetti Carbonara\n\n## Ingredients\n- 400g spaghetti\n- 200g guanciale"
    )
    private String recipeMarkdown;
}
