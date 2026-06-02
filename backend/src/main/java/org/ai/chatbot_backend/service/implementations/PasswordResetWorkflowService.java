package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.email.EmailDetails;
import org.ai.chatbot_backend.email.EmailService;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetWorkflowService {

    private final EmailService emailService;
    private final UserService userService;
    private final PasswordResetTokenService passwordResetTokenService;

    @Transactional
    public boolean sendPasswordResetEmail(EmailDetails emailDetails) {
        try {
            User user = userService.findUserByEmail(emailDetails.getRecipient());
            PasswordResetToken token = userService.generatePasswordResetTokenForUser(user);
            return emailService.sendPasswordResetEmail(emailDetails, token);
        } catch (ResourceNotFoundException e) {
            return true;
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        passwordResetTokenService.validatePasswordResetTokenOrThrow(token);

        long userId = passwordResetTokenService.findUserIdByTokenOrThrow(token);
        User user = userService.findById(userId);

        userService.changeUserPassword(user, newPassword);

        passwordResetTokenService.deleteByToken(token);
    }
}
