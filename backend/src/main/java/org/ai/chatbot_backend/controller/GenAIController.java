package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.ChatService;
import org.ai.chatbot_backend.service.ImageService;
import org.ai.chatbot_backend.service.RecipeService;
import org.springframework.ai.image.ImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
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
                                    @RequestParam int height,
                                    @RequestParam int width,
                                    @RequestParam(required = false) String course,
                                    @RequestParam(required = false) String mainIngredient,
                                    @RequestParam(required = false) String dishType) {

        if (!style.equalsIgnoreCase("vivid") && !style.equalsIgnoreCase("natural")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorry, the picked style is invalid");
        }

        String size = width + "x" + height;
        if (!size.equalsIgnoreCase("1024x1024") && !size.equalsIgnoreCase("1792x1024") &&
                !size.equalsIgnoreCase("1024x1792")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorry, the picked size is invalid");
        }

        try {
            ImageResponse imageResponse = imageService.generateDishImageFromParams(
                    name, course, mainIngredient, dishType, style, height, width);
            return ResponseEntity.ok(imageResponse.getResult().getOutput().getUrl());
        } catch (InappropriateRequestRefusalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
