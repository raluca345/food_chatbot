package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;

public interface IPasswordResetTokenService {

    PasswordResetToken saveToken(User user, String token);

    PasswordResetToken findByToken(String token);

    long findUserIdByTokenOrThrow(String token);

    void deleteByToken(String token);
}
