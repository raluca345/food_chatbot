package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.RecipeRequest;

public interface IRecipeService {
    CreateRecipeResult createRecipe(RecipeRequest request);
}
