package org.ai.chatbot_backend.service;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.ResourceNotFound;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class RecipeFileService {
    private final Map<Long, String> storage = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);


    public synchronized Long storeRecipeText(String recipeText) {
        long id = counter.incrementAndGet();
        storage.put(id, recipeText);
        return id;
    }

    public Resource getRecipeFile(Long id) {
        String recipeText = storage.get(id);
        if (recipeText == null) {
            throw new ResourceNotFound("Recipe not found.");
        }

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

    public String getDownloadMarkdown(Long id, String backendBaseUrl) {
        String url = backendBaseUrl.replaceAll("/+$", "") + "/api/v1/recipes/download/" + id;
        return "[Download recipe](" + url + ")";
    }
}