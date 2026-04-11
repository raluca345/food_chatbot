package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.FoodImageRequest;
import org.ai.chatbot_backend.dto.ImageContent;
import org.ai.chatbot_backend.model.Image;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.dto.ImageDto;
import org.ai.chatbot_backend.dto.PageResult;

public interface IImageService {
    String generateFoodImageFromParams(FoodImageRequest request);

    PageResult<ImageDto> getImages(User user, int page, int pageSize);

    void deleteById(long id);
    void deleteByIdForUser(long imageId, User user);

    Image getImageForUser(long imageId, User user);

    ImageContent loadImageContentForUser(long imageId, User user);
}
