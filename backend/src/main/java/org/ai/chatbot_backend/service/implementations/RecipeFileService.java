package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.RecipeFile;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.RecipeFileRepository;
import org.ai.chatbot_backend.repository.UserRepository;
import org.ai.chatbot_backend.service.interfaces.IRecipeFileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class RecipeFileService implements IRecipeFileService {

    private final RecipeFileRepository recipeFileRepository;
    private final UserRepository userRepository;

    @Override
    public synchronized Long storeRecipeText(String recipeText) {
        RecipeFile file = RecipeFile.builder()
                .content(recipeText)
                .build();
        RecipeFile saved = recipeFileRepository.save(file);
        return saved.getId();
    }

    @Override
    public Resource getRecipeFile(Long id) {
        RecipeFile recipeFile = recipeFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found."));

        String recipeText = recipeFile.getContent();

        try {
            File file = File.createTempFile("recipe-" + id + "-", ".txt");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(recipeText);
            }
            return new FileSystemResource(file);
        } catch (IOException e) {
            throw new RuntimeException("Error writing recipe file", e);
        }
    }

    @Override
    public String getDownloadMarkdown(Long id, String backendBaseUrl) {
        String base = backendBaseUrl == null ? "" : backendBaseUrl.trim();

        if (base.endsWith("/api/v1")) {
            base = base.substring(0, base.length() - "/api/v1".length());
        }

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String url = base + "/api/v1/recipes/download/" + id;
        return "[Download recipe](" + url + ")";
    }

    @Override
    public void attachFileToUser(Long fileId, Long userId) {
        if (fileId == null || userId == null) return;
        RecipeFile file = recipeFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe file not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (file.getUser() == null) {
            file.setUser(user);
            recipeFileRepository.save(file);
        }
    }
}