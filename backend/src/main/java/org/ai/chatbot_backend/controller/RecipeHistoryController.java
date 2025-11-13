package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.SaveRecipeHistoryRequest;
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
@RequestMapping("api/v1/recipes/history")
public class RecipeHistoryController {

    private final RecipeHistoryService recipeHistoryService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RecipeHistory> saveHistory(@RequestBody SaveRecipeHistoryRequest request,
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
}
