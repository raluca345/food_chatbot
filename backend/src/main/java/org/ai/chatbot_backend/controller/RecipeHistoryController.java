package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.exception.ResourceNotFound;
import org.ai.chatbot_backend.exception.WrongOwnerException;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.service.implementations.RecipeHistoryService;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/recipes/history")
public class RecipeHistoryController {

    private final RecipeHistoryService recipeHistoryService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RecipeHistory> saveHistory(@RequestBody SaveRecipeInHistoryRequest request,
                                                     Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        long userId = userService.findUserIdByEmail(email);
        RecipeHistory recipeHistory = recipeHistoryService.save(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeHistory);
    }

    @GetMapping
    public ResponseEntity<List<RecipeHistory>> getHistory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        long userId = userService.findUserIdByEmail(email);
        return ResponseEntity.ok(recipeHistoryService.listForUser(userId));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<RecipeHistory> deleteHistoryEntry(@PathVariable("id") long id,
                                                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        long userId = userService.findUserIdByEmail(email);
        try {
            recipeHistoryService.deleteFromHistory(userId, id);
        } catch (ResourceNotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (WrongOwnerException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
