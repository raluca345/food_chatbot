package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.SaveRecipeHistoryRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.*;
import org.springframework.ai.image.ImageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
public class GenAIController {
    private final ChatService chatService;
    private final ImageService imageService;
    private final RecipeService recipeService;
    private final RecipeFileService recipeFileService;
    private final UserService userService;
    private final RecipeHistoryService recipeHistoryService;

    @PostMapping("messages")
    public ResponseEntity<String> getResponse(@RequestParam String prompt) {
        try {
            return ResponseEntity.ok(chatService.getResponse(prompt));
        } catch (InappropriateRequestRefusalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("recipes")
    public ResponseEntity<String> generateRecipe(@RequestParam String ingredients,
                                                 @RequestParam(defaultValue = "any") String cuisine,
                                                 @RequestParam(defaultValue = "") String dietaryRestrictions,
                                                 Authentication authentication) {
        try {
            String recipe = recipeService.createRecipe(ingredients, cuisine, dietaryRestrictions);

            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                long userId = userService.findUserIdByEmail(email);

                String title = recipeService.extractRecipeTitle(recipe);

                List<String> lines = Arrays.stream(recipe.split("\\R")).toList();
                if (lines.size() > 2) {
                    lines = lines.subList(0, lines.size() - 2);
                }
                String contentWithoutLink = String.join("\n", lines);

                SaveRecipeHistoryRequest recipeHistoryRequest = new SaveRecipeHistoryRequest();
                recipeHistoryRequest.setTitle(title);
                recipeHistoryRequest.setContent(contentWithoutLink);

                recipeHistoryService.save(userId, recipeHistoryRequest);
            }

            return ResponseEntity.ok(recipe);

        } catch (InappropriateRequestRefusalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong, please try again.");
        }
    }

    @GetMapping("/recipes/download/{id}")
    public ResponseEntity<Resource> downloadRecipe(@PathVariable Long id) {
        Resource resource = recipeFileService.getRecipeFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe-" + id + ".txt")
                .body(resource);
    }


    @PostMapping("food-images")
    public ResponseEntity<String> generateFoodImage(@RequestParam(defaultValue = "") String name,
                                    @RequestParam String style,
                                    @RequestParam(defaultValue = "1024x1024") String size,
                                    @RequestParam(required = false) String course,
                                    @RequestParam(required = false) String mainIngredient,
                                    @RequestParam(required = false) String dishType) {

        if (!style.equalsIgnoreCase("vivid") && !style.equalsIgnoreCase("natural")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorry, the picked style is invalid");
        }

        if (!size.equalsIgnoreCase("1024x1024") && !size.equalsIgnoreCase("1792x1024") &&
                !size.equalsIgnoreCase("1024x1792")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorry, the picked size is invalid");
        }

        try {
            ImageResponse imageResponse = imageService.generateDishImageFromParams(
                    name, course, mainIngredient, dishType, style, size);

            if (imageResponse.getResults().isEmpty()) {
                throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
            }

            log.info(imageResponse.getResult().getOutput().getUrl());
            return ResponseEntity.ok(imageResponse.getResult().getOutput().getUrl());
        } catch (InappropriateRequestRefusalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("my-images")
    public ResponseEntity<String> gatGallery() {
        return ResponseEntity.ok("Gallery placeholder");
    }
}
