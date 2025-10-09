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


    public ImageResponse generateDishImageFromParams(String name, String course, String mainIngredient, String dishType,
                                                     String style, int height, int width) {

        String systemPrompt = "You are a helpful assistant that only generates images of food. Do not generate images" +
                " of anything else.";
        StringBuilder templateBuilder = new StringBuilder(systemPrompt +
                " I want an image of a dish with the name: {name}.");

        if (name == null) {
            name = "any";
        }

        if (course != null && !course.isBlank()) {
            templateBuilder.append(" It is a ").append("{course}").append(".");
        }
        if (mainIngredient != null && !mainIngredient.isBlank()) {
            templateBuilder.append(" The main ingredient is ").append("{mainIngredient}").append(".");
        }
        if (dishType != null && !dishType.isBlank()) {
            templateBuilder.append(" The type of dish is ").append("{dishType}").append(".");
        }
        String template = templateBuilder.toString();
        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of(
                "name", name,
                "course", course != null ? course : "",
                "mainIngredient", mainIngredient != null ? mainIngredient : "",
                "dishType", dishType != null ? dishType : ""
        );
        Prompt prompt = promptTemplate.create(params);
        try {
            return azureOpenAiImageModel.call(
                    new ImagePrompt(String.valueOf(prompt),
                            AzureOpenAiImageOptions.builder()
                                    .style(style)
                                    .height(height)
                                    .width(width).build())
            );
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }
}
