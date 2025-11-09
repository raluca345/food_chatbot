package org.ai.chatbot_backend.service.interfaces;

import org.springframework.ai.image.ImageResponse;

public interface IImageService {
    ImageResponse generateDishImageFromParams(String name, String course, String ingredients, String dishType,
                                              String style, String size);
}
