package org.ai.chatbot_backend.dto;

import lombok.Data;

@Data
public class RecipeRequest {
    private String ingredients;
    private String cuisine;
    private String dietaryRestrictions;
}
