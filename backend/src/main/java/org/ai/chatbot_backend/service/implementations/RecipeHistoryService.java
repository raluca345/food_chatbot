package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.SaveRecipeHistoryRequest;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.repository.RecipeHistoryRepository;
import org.ai.chatbot_backend.service.interfaces.IRecipeHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeHistoryService implements IRecipeHistoryService {

    private final RecipeHistoryRepository recipeHistoryRepository;

    @Override
    public RecipeHistory save(long userId, SaveRecipeHistoryRequest request) {
        RecipeHistory recipeHistory = RecipeHistory.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return recipeHistoryRepository.save(recipeHistory);
    }

    @Override
    public List<RecipeHistory> listForUser(Long userId) {
        return recipeHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
