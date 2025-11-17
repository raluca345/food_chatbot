package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetWorkflowService {

    private final UserService userService;
    private final PasswordResetTokenService passwordResetTokenService;

    @Transactional
    public void resetPassword(String token, String newPassword) {
        userService.validatePasswordResetTokenOrThrow(token);

        long userId = passwordResetTokenService.findUserIdByTokenOrThrow(token);
        User user = userService.findById(userId);

        userService.changeUserPassword(user, newPassword);

        passwordResetTokenService.deleteByToken(token);
    }
}

