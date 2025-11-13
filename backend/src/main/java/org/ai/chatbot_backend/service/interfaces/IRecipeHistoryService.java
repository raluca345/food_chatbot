package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.model.RecipeHistory;

import java.util.List;

public interface IRecipeHistoryService {
    RecipeHistory save(long userId, SaveRecipeInHistoryRequest request);
    List<RecipeHistory> listForUser(Long userId);
    void deleteFromHistory(Long userId, Long entryId);
}
