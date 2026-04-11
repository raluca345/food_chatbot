package org.ai.chatbot_backend.service.interfaces;

import org.springframework.core.io.Resource;

public interface IRecipeFileService {
    Long storeRecipeText(String recipeText);

    Resource getRecipeFileForUser(Long id, Long userId);

    Resource getRecipeFileForGuest(String recipeMarkdown);

    String getDownloadMarkdown(Long id, String backendBaseUrl);

    void attachFileToUser(Long fileId, Long userId);
}
