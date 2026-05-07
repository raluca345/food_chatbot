package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "User chat message request")
public class ChatMessageRequest {
    @Schema(
            description = "User's message content",
            example = "Can you suggest a pasta recipe?"
    )
    private String message;
}
