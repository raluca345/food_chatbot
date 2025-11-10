package org.ai.chatbot_backend.auth;

import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.implementations.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        User saved = userService.createUser(request.getUsername(), request.getEmail(), request.getPassword());
        Map<String,Object> claims = new HashMap<>();
        claims.put("name", saved.getName());
        var token = jwtService.generateToken(claims, saved);
        return AuthResponse.builder()
                .token(token)
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        String password = request.getPassword();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        password
                )
        );
        var user = userService.findUserByEmail(email);
        Map<String,Object> claims = new HashMap<>();
        claims.put("name", user.getName());
        var token = jwtService.generateToken(claims, user);
        return AuthResponse.builder()
                .token(token)
                .build();
    }
}
