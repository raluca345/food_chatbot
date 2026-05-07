package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Chat message")
public class MessageDto {
    @Schema(
            description = "Unique message identifier",
            example = "1"
    )
    private Long id;

    @Schema(
            description = "Role of the message sender (e.g., 'user' or 'assistant')",
            example = "user"
    )
    private String role;

    @Schema(
            description = "Message content",
            example = "What ingredients do I need for a pasta carbonara?"
    )
    String content;

    @Schema(
            description = "Timestamp when the message was sent",
            example = "2026-05-07T10:30:00"
    )
    LocalDateTime timestamp;
}
