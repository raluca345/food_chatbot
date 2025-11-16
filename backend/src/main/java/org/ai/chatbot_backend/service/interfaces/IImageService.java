package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.ImageContent;
import org.ai.chatbot_backend.dto.ImageDto;
import org.ai.chatbot_backend.dto.ImagePageDto;
import org.ai.chatbot_backend.model.Image;
import org.ai.chatbot_backend.model.User;

import java.util.List;

public interface IImageService {
    String generateFoodImageFromParams(String name, String course, String ingredients, String dishType,
                                              String style, String size);

    List<ImageDto> getAllImagesForUser(User user);

    ImagePageDto getImagesForUserPaged(User user, int page, int pageSize);

    void deleteById(long id);
    void deleteByIdForUser(long imageId, User user);

    Image getImageForUser(long imageId, User user);

    ImageContent loadImageContentForUser(long imageId, User user);
}
