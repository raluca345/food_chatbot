package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to update conversation title")
public class UpdateTitleRequest {
    @Schema(
            description = "New title for the conversation",
            example = "My Favorite Pizza Recipe"
    )
    private String title;
}

