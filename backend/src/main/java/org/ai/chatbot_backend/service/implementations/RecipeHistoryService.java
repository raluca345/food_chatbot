package org.ai.chatbot_backend.service.implementations;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.exception.WrongOwnerException;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.model.RecipeFile;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.RecipeHistoryRepository;
import org.ai.chatbot_backend.repository.UserRepository;
import org.ai.chatbot_backend.repository.RecipeFileRepository;
import org.ai.chatbot_backend.service.interfaces.IRecipeHistoryService;
import org.ai.chatbot_backend.service.interfaces.IRecipeFileService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipeHistoryService implements IRecipeHistoryService {

    private final RecipeHistoryRepository recipeHistoryRepository;
    private final UserRepository userRepository;
    private final RecipeFileRepository recipeFileRepository;
    private final IRecipeFileService recipeFileService;

    @Override
    public RecipeHistory save(long userId, SaveRecipeInHistoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RecipeHistory recipeHistory = RecipeHistory.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Long fileId = request.getFileId();
        if (fileId != null) {
            RecipeFile file = recipeFileRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("Recipe file not found"));
            recipeHistory.setRecipeFile(file);
        }

        RecipeHistory saved = recipeHistoryRepository.save(recipeHistory);
        recipeFileService.attachFileToUser(fileId, userId);
        return saved;
    }

    @Override
    public List<RecipeHistory> listForUser(Long userId) {
        return recipeHistoryRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void deleteFromHistory(Long userId, Long entryId) {
        RecipeHistory entry = recipeHistoryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("History entry not found"));

        if (entry.getUser() == null || entry.getUser().getId() != userId) {
            throw new WrongOwnerException("You don't have permission to delete this entry");
        }

        RecipeFile recipeFile = entry.getRecipeFile();
        Long fileId = recipeFile == null ? null : recipeFile.getId();

        recipeHistoryRepository.delete(entry);

        if (fileId != null && !recipeHistoryRepository.existsByRecipeFile_Id(fileId)) {
            try {
                Optional<RecipeFile> maybeFile = recipeFileRepository.findById(fileId);
                if (maybeFile.isPresent()) {
                    RecipeFile file = maybeFile.get();
                    User owner = file.getUser();
                    if (owner == null || owner.getId() == userId) {
                        recipeFileRepository.deleteById(fileId);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public Optional<RecipeHistory> findById(Long id) {
        return recipeHistoryRepository.findById(id);
    }
}
