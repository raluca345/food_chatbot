package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data")
public class UserDto
{
    @Schema(
            description = "Unique identifier for the user",
            example = "1"
    )
    private Long id;

    @NotEmpty
    @Schema(
            description = "User's full name",
            example = "John Doe"
    )
    private String name;

    @NotEmpty(message = "Email should not be empty")
    @Email
    @Schema(
            description = "User's email address",
            example = "john.doe@example.com"
    )
    private String email;

    @NotEmpty(message = "Password should not be empty")
    @Schema(
            description = "User's password (minimum 8 characters)",
            example = "securePassword123"
    )
    private String password;
}
