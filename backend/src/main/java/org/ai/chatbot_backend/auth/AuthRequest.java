package org.ai.chatbot_backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {

    @NotBlank
    @Email(message = "Must be a well-formatted email address")
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;
}
