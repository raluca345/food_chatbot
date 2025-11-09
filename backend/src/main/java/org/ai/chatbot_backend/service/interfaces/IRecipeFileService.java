package org.ai.chatbot_backend.service.interfaces;

import org.springframework.core.io.Resource;

public interface IRecipeFileService {
    Long storeRecipeText(String recipeText);

    Resource getRecipeFile(Long id);

    String getDownloadMarkdown(Long id, String backendBaseUrl);
}
