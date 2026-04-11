package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.MessageDto;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.enums.ConversationRole;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.Message;
import org.ai.chatbot_backend.repository.MessageRepository;
import org.ai.chatbot_backend.service.interfaces.IMessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public PageResult<MessageDto> getMessagesForConversationPaged(long conversationId, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 50;

        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<Message> p = messageRepository.findByConversationIdOrderByTimestampDesc(conversationId, pageable);

        List<MessageDto> items = p.stream()
                .map(m -> new MessageDto(m.getId(), m.getRole().name(), m.getContent(), m.getTimestamp()))
                .toList();

        return new PageResult<>(items, p.getTotalElements());
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
