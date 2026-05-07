package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password reset request")
public class PasswordDto {

    @Schema(
            description = "Password reset token received via email",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String token;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(
            description = "New password (minimum 8 characters)",
            example = "NewSecurePassword123"
    )
    private String password;
}
