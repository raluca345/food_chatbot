package org.ai.chatbot_backend.dto;

import lombok.Data;

@Data
public class FoodImageRequest {
    private String name;
    private String style;
    private String size;
    private String course;
    private String ingredients;
    private String dishType;
}
