package org.ai.chatbot_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class ConversationDto {
    private long conversationId;
    private String title;
    private List<MessageDto> messages;
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
