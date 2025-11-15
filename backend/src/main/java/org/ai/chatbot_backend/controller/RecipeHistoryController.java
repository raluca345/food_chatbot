package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.dto.RecipeHistoryDto;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.exception.WrongOwnerException;
import org.ai.chatbot_backend.model.RecipeFile;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.service.implementations.RecipeHistoryService;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.ai.chatbot_backend.service.implementations.RecipeFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/recipes/history")
public class RecipeHistoryController {

    private final RecipeHistoryService recipeHistoryService;
    private final UserService userService;
    private final RecipeFileService recipeFileService;

    @PostMapping
    public ResponseEntity<RecipeHistoryDto> saveHistory(@RequestBody SaveRecipeInHistoryRequest request,
                                                     Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        long userId = userService.findUserIdByEmail(email);
        RecipeHistory recipeHistory = recipeHistoryService.save(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(recipeHistory));
    }

    @GetMapping
    public ResponseEntity<List<RecipeHistoryDto>> getHistory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        long userId = userService.findUserIdByEmail(email);
        List<RecipeHistory> entries = recipeHistoryService.listForUser(userId);
        List<RecipeHistoryDto> dtos = entries.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteHistoryEntry(@PathVariable("id") long id,
                                                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        long userId = userService.findUserIdByEmail(email);
        try {
            recipeHistoryService.deleteFromHistory(userId, id);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (WrongOwnerException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("{id}/download")
    public ResponseEntity<Resource> downloadHistoryRecipe(@PathVariable("id") Long id,
                                                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        long userId = userService.findUserIdByEmail(email);

        RecipeHistory entry = recipeHistoryService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("History entry not found"));

        if (entry.getUser() == null || entry.getUser().getId() != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        RecipeFile file = entry.getRecipeFile();
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Long fileId = file.getId();
        Resource resource = recipeFileService.getRecipeFile(fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe-" + fileId + ".txt")
                .body(resource);
    }

    private RecipeHistoryDto toDto(RecipeHistory e) {
        Long fileId = null;
        if (e.getRecipeFile() != null) fileId = e.getRecipeFile().getId();
        return new RecipeHistoryDto(
                e.getId(),
                e.getTitle(),
                e.getContent(),
                fileId,
                e.getCreatedAt()
        );
    }
}
