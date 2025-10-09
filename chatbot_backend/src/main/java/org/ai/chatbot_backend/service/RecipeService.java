package org.ai.chatbot_backend.service;

import com.azure.core.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final ChatModel chatModel;

    public String createRecipe(String ingredients, String cuisine, String dietaryRestrictions) {
        var template = """
                I want to create a recipe using the following ingredients: {ingredients}.
                The cuisine type i prefer is: {cuisine}.
                Please consider the following dietary restrictions: {dietaryRestrictions}.
                Please provide me with a complete recipe including title, list of ingredients and cooking instructions.
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of(
                "ingredients", ingredients,
                "cuisine", cuisine,
                "dietaryRestrictions", dietaryRestrictions
        );

        Prompt prompt = promptTemplate.create(params);

        try {
            return chatModel.call(prompt).getResult().getOutput().getText();
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("I'm sorry, but I can't assist with that request.");
        }
    }
}
