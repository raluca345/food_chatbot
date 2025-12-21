package org.ai.chatbot_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    private long conversationId;
    private String title;
    private List<MessageDto> messages;
}
