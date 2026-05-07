package org.ai.chatbot_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.PasswordDto;
import org.ai.chatbot_backend.email.EmailDetails;
import org.ai.chatbot_backend.email.EmailService;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.implementations.PasswordResetWorkflowService;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.ai.chatbot_backend.service.implementations.PasswordResetTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "5. Password Reset", description = "Password reset request, token verification and password confirmation")
public class PasswordResetController {

    @Value("${app.frontend-base-url}")
    private String FRONTEND_BASE_URL;

    private final EmailService emailService;
    private final UserService userService;
    private final PasswordResetWorkflowService passwordResetWorkflowService;
    private final PasswordResetTokenService passwordResetTokenService;

    @Operation(
            summary = "Request password reset email",
            description = "Sends a password reset email if the user exists. Always returns 202 for unknown users to avoid email enumeration.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EmailDetails.class),
                            examples = @ExampleObject(
                                    name = "Password reset request",
                                    value = """
                                            {
                                              "recipient": "test@example.com",
                                              "msgBody": "Reset your password by clicking the link below.",
                                              "subject": "Reset Password"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Request accepted"),
            @ApiResponse(responseCode = "500", description = "Email failed to send")
    })
    @PostMapping("/auth/password-reset/request")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestBody EmailDetails emailDetails) {
        User user = userService.findOptionalUserByEmail(emailDetails.getRecipient()).orElse(null);
        if (user != null) {
            PasswordResetToken prt = userService.generatePasswordResetTokenForUser(user);
            boolean sent = emailService.sendPasswordResetEmail(emailDetails, prt);
            if (!sent) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Email failed to send");
            }
        }
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "Verify reset token",
            description = "Validates the reset token and redirects to the frontend password reset page."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Token valid, redirect to frontend"),
            @ApiResponse(responseCode = "404", description = "Token not found"),
            @ApiResponse(responseCode = "410", description = "Token expired")
    })
    @GetMapping("/auth/password-reset/verify")
    public ResponseEntity<?> redirectToPasswordResetPage(
            @Parameter(description = "Password reset token", required = true)
            @RequestParam String token) {
        passwordResetTokenService.validatePasswordResetTokenOrThrow(token);

        String frontendUrl = FRONTEND_BASE_URL + "/reset-password?token=" + token;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendUrl)
                .build();
    }

    @Operation(
            summary = "Change password",
            description = "Resets password using a valid token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "404", description = "Token or user not found"),
            @ApiResponse(responseCode = "410", description = "Token expired"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/auth/password-reset/confirm")
    public ResponseEntity<?> changePassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PasswordDto.class),
                            examples = @ExampleObject(
                                    name = "Password reset confirmation",
                                    value = """
                                            {
                                              "token": "1d35ff4a-2b34-457d-aa06-0ecb964e74c1",
                                              "password": "newpassword123"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody PasswordDto newPassword) {
        passwordResetWorkflowService.resetPassword(newPassword.getToken(), newPassword.getPassword());
        return ResponseEntity.noContent().build();
    }
}
