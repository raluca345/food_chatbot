package org.ai.chatbot_backend.repository;

import org.ai.chatbot_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);
}
