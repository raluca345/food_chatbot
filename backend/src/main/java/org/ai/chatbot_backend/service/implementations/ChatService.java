package org.ai.chatbot_backend.service.implementations;

import com.azure.core.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.interfaces.IChatService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService implements IChatService {
    private final ChatModel chatModel;
    private final RecipeFileService recipeFileService;

    @Value("${app.backendBaseUrl:http://localhost:8080}")
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
        return text.toLowerCase().contains("ingredients") && text.toLowerCase().contains("instructions");
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
