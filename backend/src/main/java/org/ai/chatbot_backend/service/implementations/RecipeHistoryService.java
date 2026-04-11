package org.ai.chatbot_backend.service.implementations;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.dto.RecipeHistoryDto;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.model.RecipeFile;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.RecipeHistoryRepository;
import org.ai.chatbot_backend.repository.UserRepository;
import org.ai.chatbot_backend.repository.RecipeFileRepository;
import org.ai.chatbot_backend.service.interfaces.IRecipeHistoryService;
import org.ai.chatbot_backend.service.interfaces.IRecipeFileService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeHistoryService implements IRecipeHistoryService {
    private static final Pattern TITLE_PATTERN = Pattern.compile("^###\\s*(.+)$", Pattern.MULTILINE);

    private final RecipeHistoryRepository recipeHistoryRepository;
    private final UserRepository userRepository;
    private final RecipeFileRepository recipeFileRepository;
    private final IRecipeFileService recipeFileService;

    @Override
    @Transactional
    public RecipeHistory saveGeneratedRecipe(long userId, CreateRecipeResult result) {
        if (result == null) {
            throw new ResourceNotFoundException("Recipe generation result not found");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long fileId = result.getFileId();
        RecipeFile file = null;
        if (fileId != null) {
            file = recipeFileRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("Recipe file not found"));
            if (file.getUser() != null) {
                Long ownerId = file.getUser().getId();
                if (!ownerId.equals(userId)) {
                    throw new ResourceNotFoundException("Recipe file not found");
                }
            }
        }

        RecipeHistory recipeHistory = RecipeHistory.builder()
                .user(user)
                .title(extractRecipeTitle(result.getRecipeMarkdown()))
                .content(result.contentWithoutDownload().trim())
                .recipeFile(file)
                .build();

        RecipeHistory saved = recipeHistoryRepository.save(recipeHistory);
        recipeFileService.attachFileToUser(fileId, userId);
        return saved;
    }

    private String extractRecipeTitle(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "Untitled Recipe";
        }
        Matcher matcher = TITLE_PATTERN.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Untitled Recipe";
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
            throw new AccessDeniedException("You don't have permission to delete this entry");
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

    @Override
    public PageResult<RecipeHistoryDto> getHistoryForUserPaged(Long userId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;

        List<RecipeHistory> entries = listForUser(userId);
        if (entries == null || entries.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }

        int total = entries.size();
        int start = (page - 1) * pageSize;
        if (start >= total) {
            return new PageResult<>(List.of(), total);
        }
        int end = Math.min(start + pageSize, total);

        List<RecipeHistoryDto> pageItems = entries.subList(start, end)
                .stream()
                .map(RecipeHistory::toDto)
                .toList();

        return new PageResult<>(List.copyOf(pageItems), total);
    }
}
