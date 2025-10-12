package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.ChatService;
import org.ai.chatbot_backend.service.ImageService;
import org.ai.chatbot_backend.service.RecipeService;
import org.springframework.ai.image.ImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
public class GenAIController {
    private final ChatService chatService;
    private final ImageService imageService;
    private final RecipeService recipeService;

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
                                 @RequestParam(defaultValue = "") String dietaryRestrictions) {
        try {
            return ResponseEntity.ok(recipeService.createRecipe(ingredients, cuisine, dietaryRestrictions));
        } catch (InappropriateRequestRefusalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
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
}
