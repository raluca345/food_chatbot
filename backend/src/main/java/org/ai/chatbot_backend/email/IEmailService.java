package org.ai.chatbot_backend.email;

import org.ai.chatbot_backend.model.PasswordResetToken;

public interface IEmailService {

    boolean sendSimpleMail(EmailDetails details);

    boolean sendPasswordResetEmail(EmailDetails details, PasswordResetToken passwordResetToken);
}
