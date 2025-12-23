package org.ai.chatbot_backend.service.implementations;

import com.azure.core.exception.HttpResponseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.EmptyTitleException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.ConversationRepository;
import org.ai.chatbot_backend.service.interfaces.IConversationService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService implements IConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageService messageService;


    @Override
    public Conversation createConversation(User user) {

        LocalDateTime now = LocalDateTime.now();

        Conversation conversation = Conversation.builder()
                .user(user)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return conversationRepository.save(conversation);
    }

    @Transactional
    @Override
    public Conversation createConversationWithFirstMessage(User user, String content) {

        Conversation conversation = createConversation(user);

        messageService.createUserMessage(content, conversation);

        conversation.setUpdatedAt(LocalDateTime.now());

        return conversation;
    }

    @Override
    @Transactional
    public Conversation updateTitle(User user, long id, String title) {

        if (title.isBlank()) {
            throw new EmptyTitleException("Title can't be blank");
        }

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getUser().getId() !=  user.getId()) {
            throw new AccessDeniedException("You are not allowed to update this conversation");
        }

        conversation.setTitle(title);
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }


    @Override
    public void deleteConversation(User user, long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation with id " + conversationId + " not found"));

        if (conversation.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("You are not allowed to delete this conversation");
        }

        conversationRepository.delete(conversation);
    }

    @Override
    public Conversation findById(long conversationId) {
        return conversationRepository.findById(conversationId).orElseThrow(
                ()  -> new ResourceNotFoundException("Conversation with id " + conversationId + " not found"));
    }

    @Override
    public List<Conversation> findByUser(User user) {
        return conversationRepository.findByUserId(user.getId());
    }

    @Override
    public String createTitle(ChatModel model, String userPrompt) {
        String prompt = """
                Summarize the user's request in a title that's between 5 and 30 characters long.
                """;
        String fullPrompt = prompt + "\nUser: " + userPrompt;

        try {
            return model.call(fullPrompt);
        }  catch (HttpResponseException e) {
            return "New Chat";
        }
    }
}
