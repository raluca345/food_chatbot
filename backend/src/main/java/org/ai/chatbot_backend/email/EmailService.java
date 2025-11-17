package org.ai.chatbot_backend.email;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${app.backend-base-url}")
    private String BACKEND_BASE_URL;

    @Override
    public boolean sendSimpleMail(EmailDetails details) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(details.getRecipient());
            message.setSubject(details.getSubject());
            message.setText(details.getMsgBody());
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sendPasswordResetEmail(EmailDetails details, PasswordResetToken passwordResetToken) {
        try {
            String url = BACKEND_BASE_URL + "/auth/password-reset/verify?token=" + passwordResetToken.getToken();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(details.getRecipient());
            message.setSubject(details.getSubject());
            message.setText(details.getMsgBody() + "\n\n" + url);

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
