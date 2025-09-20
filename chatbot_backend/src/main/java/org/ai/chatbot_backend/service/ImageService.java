package org.ai.chatbot_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.azure.openai.AzureOpenAiImageModel;
import org.springframework.ai.azure.openai.AzureOpenAiImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final AzureOpenAiImageModel azureOpenAiImageModel;

    public ImageResponse generateImage(String prompt, String style, int height, int width) {
        return azureOpenAiImageModel.call(
                new ImagePrompt(prompt,
                        AzureOpenAiImageOptions.builder()
                                .style(style)
                                .height(height)
                                .width(width).build())
        );
    }
}
