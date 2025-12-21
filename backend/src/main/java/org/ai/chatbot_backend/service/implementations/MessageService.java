package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.enums.ConversationRole;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.Message;
import org.ai.chatbot_backend.repository.MessageRepository;
import org.ai.chatbot_backend.service.interfaces.IMessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService implements IMessageService {

    private final MessageRepository messageRepository;

    @Override
    public Message createUserMessage(String content, Conversation conversation) {

        Message message = Message.builder()
                .content(content)
                .role(ConversationRole.USER)
                .conversation(conversation)
                .timestamp(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    @Override
    public Message createAssistantMessage(String assistantReply, Conversation conversation) {

        Message message = Message.builder()
                .content(assistantReply)
                .role(ConversationRole.ASSISTANT)
                .conversation(conversation)
                .timestamp(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

}
