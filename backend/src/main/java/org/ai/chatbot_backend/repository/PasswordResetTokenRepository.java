package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    PasswordResetToken findByToken(String token);

    @Query("SELECT t.user.id FROM PasswordResetToken t WHERE t.token = :token")
    Optional<Long> findUserIdByToken(@Param("token") String token);

    void deleteByToken(String token);
}
