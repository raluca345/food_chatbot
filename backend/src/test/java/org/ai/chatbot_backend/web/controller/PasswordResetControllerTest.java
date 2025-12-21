package org.ai.chatbot_backend.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.controller.PasswordResetController;
import org.ai.chatbot_backend.dto.PasswordDto;
import org.ai.chatbot_backend.email.EmailDetails;
import org.ai.chatbot_backend.email.EmailService;
import org.ai.chatbot_backend.enums.UserRole;
import org.ai.chatbot_backend.exception.PasswordResetTokenExpiredException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.implementations.PasswordResetTokenService;
import org.ai.chatbot_backend.service.implementations.PasswordResetWorkflowService;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.backend-base-url}")
    private String BACKEND_BASE_URL;
    @Value("${app.frontend-base-url}")
    private String FRONTEND_BASE_URL;

    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PasswordResetTokenService passwordResetTokenService;

    @MockitoBean
    private PasswordResetWorkflowService passwordResetWorkflowService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper mapper = new ObjectMapper();

    private User mockUser() {
        return User.builder()
                .id(1L)
                .name("test")
                .email("test@example.com")
                .password("pw")
                .role(UserRole.USER)
                .build();
    }

    @Nested
    class RequestPasswordResetEmailTests {
        @Test
        void whenValidUserEmail_thenSendPasswordResetLink() throws Exception {
            User mockUser = mockUser();
            PasswordResetToken passwordResetToken = new PasswordResetToken(1L, "123", mockUser,
                    Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));

            String passwordResetEmail = "Click this to reset your password\n\n" + BACKEND_BASE_URL +
                    "/auth/password-reset/verify?token=123";

            EmailDetails emailDetails = new EmailDetails();
            emailDetails.setSubject("Password reset link");
            emailDetails.setRecipient(mockUser.getEmail());
            emailDetails.setMsgBody(passwordResetEmail);

            String json = mapper.writeValueAsString(emailDetails);

            when(userService.findUserByEmail(mockUser.getEmail())).thenReturn(mockUser);
            when(userService.generatePasswordResetTokenForUser(mockUser)).thenReturn(passwordResetToken);
            when(emailService.sendPasswordResetEmail(any(), any()))
                    .then(inv -> {
                        EmailDetails details = inv.getArgument(0);
                        assertThat(details.getMsgBody()).contains("/auth/password-reset/verify?token=123");
                        return true;
                    });

            mockMvc.perform(post("/api/v1/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isAccepted());

            verify(emailService, times(1)).sendPasswordResetEmail(eq(emailDetails), eq(passwordResetToken));
        }

        @Test
        void whenInvalidUserEmail_thenFailSilently() throws Exception {
            User mockUser = mockUser();
            when(userService.findUserByEmail(mockUser.getEmail())).thenThrow(ResourceNotFoundException.class);

            EmailDetails emailDetails = new EmailDetails();
            emailDetails.setSubject("Password reset link");
            emailDetails.setRecipient(mockUser.getEmail());
            emailDetails.setMsgBody("ignored");

            String json = mapper.writeValueAsString(emailDetails);

            mockMvc.perform(post("/api/v1/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isAccepted());

            verify(emailService, times(0)).sendPasswordResetEmail(any(), any());
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
            dto.setPassword("bad");
            String json = mapper.writeValueAsString(dto);

            doThrow(IllegalStateException.class).when(passwordResetWorkflowService)
                    .resetPassword(dto.getToken(), dto.getPassword());

            mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(passwordResetWorkflowService, times(1)).resetPassword(dto.getToken(), dto.getPassword());
        }
    }
}
