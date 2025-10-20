package org.ai.chatbot_backend.service;

import com.azure.core.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final ChatModel chatModel;
    private final RecipeFileService recipeFileService;

    @Value("${app.backendBaseUrl:http://localhost:8080}")
    private String backendBaseUrl;

    private static Prompt getSystemPrompt(String ingredients, String cuisine, String dietaryRestrictions) {
        var template = """
                I want to create a recipe using the following ingredients: {ingredients}.
                The cuisine type I prefer is: {cuisine}.
                Please consider the following dietary restrictions: {dietaryRestrictions}.
                Please provide me with a complete recipe including title, list of ingredients, and cooking instructions.
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of(
                "ingredients", ingredients,
                "cuisine", cuisine,
                "dietaryRestrictions", dietaryRestrictions
        );

        return promptTemplate.create(params);
    }

    public String createRecipe(String ingredients, String cuisine, String dietaryRestrictions) {
        Prompt prompt = getSystemPrompt(ingredients, cuisine, dietaryRestrictions);

        try {
            String recipeText = chatModel.call(prompt).getResult().getOutput().getText();

            Long id = recipeFileService.storeRecipeText(recipeText);
            String downloadUrl = recipeFileService.getDownloadMarkdown(id, backendBaseUrl);

            return recipeText + "\n\nYou can download this recipe here: " + downloadUrl;
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("I'm sorry, but I can't assist with that request.");
        }
    }
}
