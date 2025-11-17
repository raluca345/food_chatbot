package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.exception.UserNotFoundException;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.PasswordResetTokenRepository;
import org.ai.chatbot_backend.service.interfaces.IPasswordResetTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService implements IPasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public PasswordResetToken saveToken(User user, String token) {
        Date expirationDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expirationDate)
                .build();

        return passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public PasswordResetToken findByToken(String token) {
        return passwordResetTokenRepository.findByToken(token);
    }

    @Override
    public long findUserIdByTokenOrThrow(String token) {
        return passwordResetTokenRepository
                .findUserIdByToken(token).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        passwordResetTokenRepository.deleteByToken(token);
    }
}
