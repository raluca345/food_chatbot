package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.Message;
import org.ai.chatbot_backend.enums.ConversationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message,Long> {
    List<Message> findByRole(ConversationRole role);
}
