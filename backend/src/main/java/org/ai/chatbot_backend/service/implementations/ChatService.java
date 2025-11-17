package org.ai.chatbot_backend.service.implementations;

import com.azure.core.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.interfaces.IChatService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChatService implements IChatService {
    private final ChatModel chatModel;
    private final RecipeFileService recipeFileService;

    private static final Pattern RECIPE_PATTERN = Pattern.compile(
            "(?s)^###\\s+.+?\\R+####\\s+Ingredients:.*?####\\s+Instructions:",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    @Override
    public String systemPrompt() {
        return "You are a helpful assistant that only answers questions about food, recipes, ingredients, and cooking. " +
                "If the user asks to download a recipe, always use the backend API base URL: [Download Recipe]("
                + backendBaseUrl + "/api/v1/recipes/download/{recipeId}). " +
                "Never use the frontend domain. " +
                "If the question is not about food, politely respond: 'Sorry, I can only answer questions about food.'";
    }

    @Override
    public boolean looksLikeRecipe(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        if (text.toLowerCase().contains("i cannot") ||
                text.toLowerCase().contains("i'm sorry") ||
                text.toLowerCase().contains("unable to") ||
                text.toLowerCase().contains("clarify") ||
                text.toLowerCase().contains("illegal") ||
                text.toLowerCase().contains("inappropriate")) {
            return false;
        }

        if (RECIPE_PATTERN.matcher(text).find()) {
            return true;
        }

        String lower = text.toLowerCase();
        boolean hasTitle = text.trim().startsWith("###");
        boolean hasIngredients = lower.contains("ingredients");
        boolean hasInstructions = lower.contains("instructions");

        return hasTitle && hasIngredients && hasInstructions;
    }

    @Override
    public String createDownloadableRecipe(String recipeText) {
        Long id = recipeFileService.storeRecipeText(recipeText);
        return recipeFileService.getDownloadMarkdown(id, backendBaseUrl);
    }

    @Override
    public String getResponse(String userPrompt) {
        String fullPrompt = systemPrompt() + "\nUser: " + userPrompt;
        try {
            String modelOut = chatModel.call(fullPrompt);
            if (looksLikeRecipe(modelOut)) {
                String mdLink = createDownloadableRecipe(modelOut);
                modelOut = modelOut + "\n\nYou can download this recipe here: " + mdLink;
            }
            return modelOut;
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }
}
