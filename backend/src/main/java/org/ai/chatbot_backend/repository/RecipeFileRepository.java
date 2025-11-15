package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.RecipeFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeFileRepository extends JpaRepository<RecipeFile, Long> {
}

