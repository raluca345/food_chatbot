package org.ai.chatbot_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Conversation containing chat messages")
public class ConversationDto {
    @Schema(
            description = "Unique conversation identifier",
            example = "1"
    )
    private long conversationId;

    @Schema(
            description = "Conversation title",
            example = "Pasta Recipes Discussion"
    )
    private String title;

    @Schema(
            description = "List of messages in the conversation"
    )
    private List<MessageDto> messages;

    @Schema(
            description = "Total number of messages in conversation",
            example = "10"
    )
    private Long total;

    public ConversationDto(long conversationId, String title, List<MessageDto> messages) {
        this.conversationId = conversationId;
        this.title = title;
        this.messages = messages;
        this.total = null;
    }

    public ConversationDto(long conversationId, String title, List<MessageDto> messages, Long total) {
        this.conversationId = conversationId;
        this.title = title;
        this.messages = messages;
        this.total = total;
    }
}
