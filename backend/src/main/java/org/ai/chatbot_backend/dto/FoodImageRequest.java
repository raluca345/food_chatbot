package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to generate a food image")
public class FoodImageRequest {
    @Schema(
            description = "Name or title of the dish",
            example = "Spaghetti Carbonara"
    )
    private String name;

    @Schema(
            description = "Visual style of the image",
            example = "vivid or natural"
    )
    private String style;

    @Schema(
            description = "Size or dimensions of the image",
            example = "1024x1024"
    )
    private String size;

    @Schema(
            description = "Course type",
            example = "main course"
    )
    private String course;

    @Schema(
            description = "Comma-separated list of main ingredients",
            example = "pasta, eggs, bacon, parmesan"
    )
    private String ingredients;

    @Schema(
            description = "Type of dish",
            example = "pasta"
    )
    private String dishType;
}
