package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.PasswordDto;
import org.ai.chatbot_backend.email.EmailDetails;
import org.ai.chatbot_backend.email.EmailService;
import org.ai.chatbot_backend.exception.PasswordResetTokenExpiredException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.implementations.PasswordResetWorkflowService;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
public class PasswordResetController {

    @Value("${app.frontend-base-url}")
    private String FRONTEND_BASE_URL;

    private final EmailService emailService;
    private final UserService userService;
    private final PasswordResetWorkflowService passwordResetWorkflowService;

    @PostMapping("/auth/password-reset/request")
    public ResponseEntity<Void> sendPasswordResetEmail(@RequestBody EmailDetails emailDetails) {
        try {
            User user = userService.findUserByEmail(emailDetails.getRecipient());
            if (user != null) {
                PasswordResetToken prt = userService.generatePasswordResetTokenForUser(user);
                emailService.sendPasswordResetEmail(emailDetails, prt);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/auth/password-reset/verify")
    public ResponseEntity<Void> redirectToPasswordResetPage(@RequestParam String token) {
        try {
            userService.validatePasswordResetTokenOrThrow(token);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (PasswordResetTokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        String frontendUrl = FRONTEND_BASE_URL + "/reset-password?token=" + token;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendUrl)
                .build();
    }

    @PostMapping("/auth/password-reset/confirm")
    public ResponseEntity<Void> changePassword(@RequestBody PasswordDto newPassword) {
        try {
            // Single atomic operation inside a transaction
            passwordResetWorkflowService.resetPassword(newPassword.getToken(), newPassword.getPassword());
            return ResponseEntity.noContent().build();
        } catch (PasswordResetTokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }  catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
