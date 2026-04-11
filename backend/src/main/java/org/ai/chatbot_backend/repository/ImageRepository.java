package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Page<Image> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

