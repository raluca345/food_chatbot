package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.dto.RecipeHistoryDto;
import org.ai.chatbot_backend.dto.RecipeHistoryPageDto;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.model.RecipeFile;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.interfaces.IRecipeHistoryService;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.ai.chatbot_backend.service.implementations.RecipeFileService;
import org.ai.chatbot_backend.security.AuthHelper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/recipes/history")
public class RecipeHistoryController {

    private final IRecipeHistoryService recipeHistoryService;
    private final UserService userService;
    private final RecipeFileService recipeFileService;
    private final AuthHelper authHelper;

    @PostMapping
    public ResponseEntity<RecipeHistoryDto> saveHistory(@RequestBody SaveRecipeInHistoryRequest request,
                                                     Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RecipeHistory recipeHistory = recipeHistoryService.save(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeHistory.toDto());
    }

    @GetMapping
    public ResponseEntity<?> getHistory(Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        RecipeHistoryPageDto pageDto = recipeHistoryService.getHistoryForUserPaged(user.getId(), page, pageSize);
        return ResponseEntity.ok(pageDto);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteHistoryEntry(@PathVariable("id") long id,
                                                            Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            recipeHistoryService.deleteFromHistory(user.getId(), id);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("{id}/download")
    public ResponseEntity<Resource> downloadHistoryRecipe(@PathVariable("id") Long id,
                                                          Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        RecipeHistory entry = recipeHistoryService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("History entry not found"));
        if (entry.getUser() == null || entry.getUser().getId() != user.getId()) {
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
}
