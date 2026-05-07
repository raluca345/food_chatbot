package org.ai.chatbot_backend.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Email details")
public class EmailDetails {

    @Schema(
            description = "Recipient email address",
            example = "user@example.com"
    )
    private String recipient;

    @Schema(
            description = "Email message body/content",
            example = "Your password reset link has been sent to your email."
    )
    private String msgBody;

    @Schema(
            description = "Email subject line",
            example = "Password Reset Request"
    )
    private String subject;
}
