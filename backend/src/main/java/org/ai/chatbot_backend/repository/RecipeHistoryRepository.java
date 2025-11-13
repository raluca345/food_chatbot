package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.RecipeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeHistoryRepository extends JpaRepository<RecipeHistory, Long> {
    List<RecipeHistory> findByUser_IdOrderByCreatedAtDesc(Long userId);
    void deleteByUser_IdAndId(Long userId, Long entryId);
    boolean existsByUser_IdAndId(Long userId, Long entryId);
}
