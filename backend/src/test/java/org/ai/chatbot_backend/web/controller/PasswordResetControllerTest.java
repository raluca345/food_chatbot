package org.ai.chatbot_backend.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.auth.PasswordResetController;
import org.ai.chatbot_backend.dto.PasswordDto;
import org.ai.chatbot_backend.email.EmailDetails;
import org.ai.chatbot_backend.exception.PasswordResetTokenExpiredException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.service.implementations.PasswordResetTokenService;
import org.ai.chatbot_backend.service.implementations.PasswordResetWorkflowService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.frontend-base-url}")
    private String FRONTEND_BASE_URL;

    @MockitoBean
    private PasswordResetTokenService passwordResetTokenService;

    @MockitoBean
    private PasswordResetWorkflowService passwordResetWorkflowService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    class RequestPasswordResetEmailTests {
        @Test
        void whenValidUserEmail_thenSendPasswordResetLink() throws Exception {
            EmailDetails emailDetails = new EmailDetails();
            emailDetails.setSubject("Password reset link");
            emailDetails.setRecipient("test@example.com");
            emailDetails.setMsgBody("Click this to reset your password");

            String json = mapper.writeValueAsString(emailDetails);

            when(passwordResetWorkflowService.sendPasswordResetEmail(emailDetails)).thenReturn(true);

            mockMvc.perform(post("/api/v1/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isAccepted());

            verify(passwordResetWorkflowService, times(1)).sendPasswordResetEmail(emailDetails);
        }

        @Test
        void whenInvalidUserEmail_thenFailSilently() throws Exception {
            EmailDetails emailDetails = new EmailDetails();
            emailDetails.setSubject("Password reset link");
            emailDetails.setRecipient("unknown@example.com");
            emailDetails.setMsgBody("ignored");

            String json = mapper.writeValueAsString(emailDetails);
            when(passwordResetWorkflowService.sendPasswordResetEmail(emailDetails)).thenReturn(true);

            mockMvc.perform(post("/api/v1/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isAccepted());

            verify(passwordResetWorkflowService, times(1)).sendPasswordResetEmail(emailDetails);
        }

        @Test
        void whenEmailSendFails_thenReturnInternalServerError() throws Exception {
            EmailDetails emailDetails = new EmailDetails();
            emailDetails.setSubject("Password reset link");
            emailDetails.setRecipient("test@example.com");
            emailDetails.setMsgBody("Reset your password");

            String json = mapper.writeValueAsString(emailDetails);

            when(passwordResetWorkflowService.sendPasswordResetEmail(emailDetails)).thenReturn(false);

            mockMvc.perform(post("/api/v1/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Email failed to send"));

            verify(passwordResetWorkflowService, times(1)).sendPasswordResetEmail(emailDetails);
        }
    }

    @Nested
    class VerifyPasswordResetTokenTests {
        @Test
        void whenValidPasswordResetToken_thenRedirectToPasswordResetPage() throws Exception {
            when(passwordResetTokenService.validatePasswordResetTokenOrThrow("123")).thenReturn(true);
            String frontendUrl = FRONTEND_BASE_URL + "/reset-password?token=123";

            mockMvc.perform(get("/api/v1/auth/password-reset/verify?token=123")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", frontendUrl));
        }

        @Test
        void whenInvalidPasswordResetToken_thenReturnNotFound() throws Exception {
            when(passwordResetTokenService.validatePasswordResetTokenOrThrow("123")).thenThrow(ResourceNotFoundException.class);

            mockMvc.perform(get("/api/v1/auth/password-reset/verify?token=123")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void whenExpiredPasswordResetToken_thenReturnGone() throws Exception {
            when(passwordResetTokenService.validatePasswordResetTokenOrThrow("123")).thenThrow(PasswordResetTokenExpiredException.class);

            mockMvc.perform(get("/api/v1/auth/password-reset/verify?token=123")
                            .with(csrf()))
                    .andExpect(status().isGone());
        }
    }

    @Nested
    class ConfirmPasswordResetTests {
        @Test
        void whenValidTokenAndPassword_thenReturnNoContent() throws Exception {
            PasswordDto dto = new PasswordDto();
            dto.setToken("valid-token");
            dto.setPassword("NewSecureP@ssw0rd");
            String json = mapper.writeValueAsString(dto);

            doNothing().when(passwordResetWorkflowService).resetPassword(dto.getToken(), dto.getPassword());

            mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(passwordResetWorkflowService, times(1)).resetPassword(dto.getToken(), dto.getPassword());
        }

        @Test
        void whenExpiredToken_thenReturnGone() throws Exception {
            PasswordDto dto = new PasswordDto();
            dto.setToken("expired-token");
            dto.setPassword("irrelevant");
            String json = mapper.writeValueAsString(dto);

            doThrow(PasswordResetTokenExpiredException.class).when(passwordResetWorkflowService)
                    .resetPassword(dto.getToken(), dto.getPassword());

            mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isGone());

            verify(passwordResetWorkflowService, times(1)).resetPassword(dto.getToken(), dto.getPassword());
        }

        @Test
        void whenInvalidToken_thenReturnNotFound() throws Exception {
            PasswordDto dto = new PasswordDto();
            dto.setToken("invalid-token");
            dto.setPassword("irrelevant");
            String json = mapper.writeValueAsString(dto);

            doThrow(ResourceNotFoundException.class).when(passwordResetWorkflowService)
                    .resetPassword(dto.getToken(), dto.getPassword());

            mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(passwordResetWorkflowService, times(1)).resetPassword(dto.getToken(), dto.getPassword());
        }

        @Test
        void whenGenericFailure_thenReturnBadRequest() throws Exception {
            PasswordDto dto = new PasswordDto();
            dto.setToken("some-token");
            dto.setPassword("badpassword");
            String json = mapper.writeValueAsString(dto);

            doThrow(IllegalStateException.class).when(passwordResetWorkflowService)
                    .resetPassword(dto.getToken(), dto.getPassword());

            mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Internal server error"));

            verify(passwordResetWorkflowService, times(1)).resetPassword(dto.getToken(), dto.getPassword());
        }

        @Test
        void whenPasswordIsTooLong_thenReturnBadRequest() throws Exception {
            PasswordDto dto = new PasswordDto();
            dto.setToken("some-token");
            dto.setPassword("a".repeat(73));
            String json = mapper.writeValueAsString(dto);

            mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Password must be between 8 and 72 characters"));

            verify(passwordResetWorkflowService, never()).resetPassword(any(), any());
        }
    }
}
