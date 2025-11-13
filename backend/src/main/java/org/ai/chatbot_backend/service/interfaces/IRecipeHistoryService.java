package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.SaveRecipeHistoryRequest;
import org.ai.chatbot_backend.model.RecipeHistory;

import java.util.List;

public interface IRecipeHistoryService {
    RecipeHistory save(long userId, SaveRecipeHistoryRequest request);
    List<RecipeHistory> listForUser(Long userId);
}
