package org.ai.chatbot_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecipeResponse {
    private String title;

    @JsonProperty("recipe_markdown")
    private String recipeMarkdown;
}
