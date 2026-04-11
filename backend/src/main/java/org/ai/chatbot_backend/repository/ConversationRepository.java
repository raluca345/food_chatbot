package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId,
                                                        Pageable pageable);
}
