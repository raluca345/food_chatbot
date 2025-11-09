package org.ai.chatbot_backend.service.interfaces;

public interface IChatService {

    String systemPrompt();

    boolean looksLikeRecipe(String text);

    String createDownloadableRecipe(String recipeText);

    String getResponse(String userPrompt);
}
