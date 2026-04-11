package org.ai.chatbot_backend.service.interfaces;

import jakarta.transaction.Transactional;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.User;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.domain.Page;

public interface IConversationService {
    Conversation createConversation(User user);

    @Transactional
    Conversation createConversationWithFirstMessage(User user, String content);

    Conversation updateTitle(User user, long id, String title);

    void deleteConversation(User user, long conversationId);

    Conversation findById(long conversationId);

    Page<Conversation> findByUser(User user, int page, int pageSize);

    String createTitle(ChatModel model, String userPrompt);
}
