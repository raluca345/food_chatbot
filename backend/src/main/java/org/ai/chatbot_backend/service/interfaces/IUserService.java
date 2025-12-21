package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.UserDto;
import org.ai.chatbot_backend.model.PasswordResetToken;
import org.ai.chatbot_backend.model.User;

import java.util.List;

public interface IUserService {
    void saveUser(UserDto userDto);

    User findUserByEmail(String email);

    long findUserIdByEmail(String name);

    List<UserDto> findAllUsers();

    User findById(Long userId);

    PasswordResetToken generatePasswordResetTokenForUser(User user);

    void changeUserPassword(User user, String password);
}
