package org.ai.chatbot_backend.service.interfaces;

public interface IRecipeService {
    String createRecipe(String ingredients, String cuisine, String dietaryRestrictions);
}
