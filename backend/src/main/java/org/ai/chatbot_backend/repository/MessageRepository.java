package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message,Long> {
}
