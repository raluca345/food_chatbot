package org.ai.chatbot_backend.security;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;

@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final UserService userService;

    public User getAuthenticatedUserOrNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User u) return u;
        if (principal instanceof UserDetails ud) {
            try {
                return userService.findUserByEmail(ud.getUsername());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
