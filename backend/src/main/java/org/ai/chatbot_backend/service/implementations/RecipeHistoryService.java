package org.ai.chatbot_backend.service.implementations;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.exception.ResourceNotFound;
import org.ai.chatbot_backend.exception.WrongOwnerException;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.RecipeHistoryRepository;
import org.ai.chatbot_backend.repository.UserRepository;
import org.ai.chatbot_backend.service.interfaces.IRecipeHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeHistoryService implements IRecipeHistoryService {

    private final RecipeHistoryRepository recipeHistoryRepository;
    private final UserRepository userRepository;

    @Override
    public RecipeHistory save(long userId, SaveRecipeInHistoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFound("User not found"));

        RecipeHistory recipeHistory = RecipeHistory.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return recipeHistoryRepository.save(recipeHistory);
    }

    @Override
    public List<RecipeHistory> listForUser(Long userId) {
        return recipeHistoryRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void deleteFromHistory(Long userId, Long entryId) {
        if (!recipeHistoryRepository.existsById(entryId)) {
            throw new ResourceNotFound("History entry not found");
        }

        boolean owned = recipeHistoryRepository.existsByUser_IdAndId(userId, entryId);
        if (!owned) {
            throw new WrongOwnerException("You don't have permission to delete this entry");
        }

        recipeHistoryRepository.deleteByUser_IdAndId(userId, entryId);
    }
}
