package org.ai.chatbot_backend.service.implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.UserDto;
import org.ai.chatbot_backend.model.Role;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.UserRepository;
import org.ai.chatbot_backend.service.interfaces.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.lang.module.ResolutionException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        user.setRole(Role.USER);
        userRepository.save(user);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResolutionException("User not found"));
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields");
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            log.debug("Attempt to register duplicate email: {}", trimmedEmail);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        String encoded = passwordEncoder.encode(rawPassword);
        User user = User.builder()
                .name(trimmedName)
                .email(trimmedEmail)
                .password(encoded)
                .role(Role.USER)
                .build();
        User saved = userRepository.save(user);
        log.debug("Registered user id={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }

}
