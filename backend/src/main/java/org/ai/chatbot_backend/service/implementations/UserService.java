package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.UserDto;
import org.ai.chatbot_backend.exception.DuplicateEmailException;
import org.ai.chatbot_backend.exception.InvalidUserDataException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.enums.UserRole;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.UserRepository;
import org.ai.chatbot_backend.service.interfaces.IUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenService passwordResetTokenService;

    @Override
    public void saveUser(UserDto userDto) {
        createUser(userDto.getName(), userDto.getEmail(), userDto.getPassword());
    }

    @Override
    public User findUserByEmail(String email) {
        String normalized = email == null ? null : email.trim().toLowerCase();
        return userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public long findUserIdByEmail(String email) {
        String normalized = email == null ? null : email.trim().toLowerCase();
        return userRepository.findIdByEmail(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("User id not found"));
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    public UserDto mapToUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setPassword(user.getPassword());
        return userDto;
    }

    public User createUser(String name, String email, String rawPassword) {
        String trimmedEmail = email == null ? null : email.trim().toLowerCase();
        String trimmedName = name == null ? null : name.trim();
        if (trimmedEmail == null || rawPassword == null || trimmedName == null) {
            throw new InvalidUserDataException("Missing required fields");
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            log.debug("Attempt to register duplicate email: {}", trimmedEmail);
            throw new DuplicateEmailException("Email already registered");
        }
        String encoded = passwordEncoder.encode(rawPassword);
        User user = User.builder()
                .name(trimmedName)
                .email(trimmedEmail)
                .password(encoded)
                .role(UserRole.USER)
                .build();
        User saved = userRepository.save(user);
        log.debug("Registered user id={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public PasswordResetToken generatePasswordResetTokenForUser(User user) {
        String token = UUID.randomUUID().toString();

        return passwordResetTokenService.saveToken(user, token);
    }

    @Override
    public void changeUserPassword(User user, String password) {
        if (password == null) {
            throw new ResourceNotFoundException("Missing required fields");
        }

        if (passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password.");
        }

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }
}
