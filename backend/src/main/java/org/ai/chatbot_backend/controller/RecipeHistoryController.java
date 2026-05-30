package org.ai.chatbot_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.dto.RecipeHistoryDto;
import org.ai.chatbot_backend.model.RecipeHistory;
import org.ai.chatbot_backend.model.RecipeFile;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.interfaces.IRecipeHistoryService;
import org.ai.chatbot_backend.service.implementations.RecipeFileService;
import org.ai.chatbot_backend.security.AuthHelper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/recipes/history")
@Tag(name = "3. Recipe History", description = "Authenticated user's generated recipe history")
public class RecipeHistoryController {

    private final IRecipeHistoryService recipeHistoryService;
    private final RecipeFileService recipeFileService;
    private final AuthHelper authHelper;

    @Operation(
            summary = "Get recipe history",
            description = "Returns paged recipe history entries for the authenticated user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<?> getHistory(Authentication authentication,
            @Parameter(description = "1-based page number", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int pageSize) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        PageResult<RecipeHistoryDto> pageDto = recipeHistoryService.getHistoryForUserPaged(user.getId(), page, pageSize);
        return ResponseEntity.ok(pageDto);
    }

    @Operation(
            summary = "Delete recipe history entry",
            description = "Deletes a history entry and its associated recipe file for the authenticated user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "History entry deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "History entry not found")
    })
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteHistoryEntry(
            @Parameter(description = "History entry id", example = "23")
            @PathVariable long id,
                                                Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        recipeHistoryService.deleteFromHistory(user.getId(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Download recipe from history",
            description = "Downloads a recipe file from user's recipe history."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recipe file downloaded"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "History entry or recipe file not found")
    })
    @GetMapping("{id}/download")
    public ResponseEntity<?> downloadHistoryRecipe(
            @Parameter(description = "History entry id", example = "23")
            @PathVariable Long id,
                                                   Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        RecipeHistory entry = recipeHistoryService.findById(id).orElse(null);
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("History entry not found");
        }
        if (entry.getUser() == null || entry.getUser().getId() != user.getId()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("History entry not found");
        }
        RecipeFile file = entry.getRecipeFile();
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recipe file not found");
        }
        Long fileId = file.getId();
        Resource resource = recipeFileService.getRecipeFileForUser(fileId, user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe-" + fileId + ".txt")
                .body(resource);
    }
}
