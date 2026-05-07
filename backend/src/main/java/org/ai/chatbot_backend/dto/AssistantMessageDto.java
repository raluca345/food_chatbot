package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI assistant message response")
public class AssistantMessageDto {
    @Schema(
            description = "Conversation ID this message belongs to",
            example = "1"
    )
    Long conversationId;

    @Schema(
            description = "Assistant's message content",
            example = "Here's a delicious Spaghetti Carbonara recipe..."
    )
    String assistantMessage;
}
