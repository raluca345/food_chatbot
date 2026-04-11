package org.ai.chatbot_backend.web.controller;

import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.controller.UserGalleryController;
import org.ai.chatbot_backend.enums.UserRole;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserGalleryController.class)
class UserGalleryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthHelper authHelper;

    private User mockUser() {
        return User.builder()
                .id(1L)
                .name("test")
                .email("test@example.com")
                .password("pw")
                .role(UserRole.USER)
                .build();
    }

    @Test
    void deleteImage_success_returnsNoContent() throws Exception {
        User user = mockUser();
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))).thenReturn(user);

        doNothing().when(imageService).deleteByIdForUser(123L, user);

        mockMvc.perform(delete("/api/v1/users/me/images/123").with(csrf()).with(authentication(
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
                )))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteImage_forbidden_returnsForbidden() throws Exception {
        User user = mockUser();
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))).thenReturn(user);

        doThrow(new AccessDeniedException("Forbidden"))
                .when(imageService).deleteByIdForUser(123L, user);

        mockMvc.perform(delete("/api/v1/users/me/images/123").with(csrf()).with(authentication(
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
                )))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteImage_unauthenticated_returnsUnauthorized() throws Exception {
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))).thenReturn(null);
        mockMvc.perform(delete("/api/v1/users/me/images/123").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
