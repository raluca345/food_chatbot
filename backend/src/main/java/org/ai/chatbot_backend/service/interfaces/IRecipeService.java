package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.CreateRecipeResult;

public interface IRecipeService {
    CreateRecipeResult createRecipe(String ingredients, String cuisine, String dietaryRestrictions);
}
