package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.RecipeHistoryPageDto;
import org.ai.chatbot_backend.model.RecipeHistory;

import java.util.List;
import java.util.Optional;

public interface IRecipeHistoryService {
    RecipeHistory saveGeneratedRecipe(long userId, CreateRecipeResult result);
    List<RecipeHistory> listForUser(Long userId);
    void deleteFromHistory(Long userId, Long entryId);
    Optional<RecipeHistory> findById(Long id);

    RecipeHistoryPageDto getHistoryForUserPaged(Long userId, int page, int pageSize);
}
