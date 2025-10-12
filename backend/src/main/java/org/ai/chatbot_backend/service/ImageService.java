package org.ai.chatbot_backend.service;

import com.azure.core.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.azure.openai.AzureOpenAiImageModel;
import org.springframework.ai.azure.openai.AzureOpenAiImageOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;

import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final AzureOpenAiImageModel azureOpenAiImageModel;


    public ImageResponse generateDishImageFromParams(String name, String course, String ingredients, String dishType,
                                                     String style, String size) {

        String systemPrompt = "You are a helpful assistant that only generates images of food. Do not generate images" +
                " of anything else.";
        StringBuilder templateBuilder = new StringBuilder(systemPrompt +
                " I want an image of a dish with the name: {name}.");

        if (name == null || name.isBlank() || name.equals("null")) {
            name = "any";
        }

        if (course != null && !course.isBlank()) {
            templateBuilder.append(" It is a ").append("{course}").append(".");
        }
        if (ingredients != null && !ingredients.isBlank()) {
            templateBuilder.append(" The ingredients are ").append("{ingredients}").append(".");
        }
        if (dishType != null && !dishType.isBlank()) {
            templateBuilder.append(" The type of dish is ").append("{dishType}").append(".");
        }
        String template = templateBuilder.toString();
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of(
                "name", name,
                "course", course != null ? course : "",
                "ingredients", ingredients != null ? ingredients : "",
                "dishType", dishType != null ? dishType : ""
        );
        Prompt prompt = promptTemplate.create(params);
        int width, height;
        try {
            String[] parts = size.toLowerCase().split("x");
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            throw new InappropriateRequestRefusalException("Sorry, the picked size is invalid");
        }
        try {
            ImageResponse response = azureOpenAiImageModel.call(
                    new ImagePrompt(String.valueOf(prompt),
                            AzureOpenAiImageOptions.builder()
                                    .style(style)
                                    .width(width)
                                    .height(height)
                                    .build())
            );
            if (response.getResults().isEmpty()) {
                throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
            }
            return response;
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }
}
