package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.AssistantMessageDto;
import org.ai.chatbot_backend.dto.ConversationDto;
import org.ai.chatbot_backend.dto.MessageDto;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.model.User;

public interface IChatService {

    String systemPrompt();

    boolean looksLikeRecipe(String text);

    String createDownloadableRecipe(String recipeText, Long userId);

    AssistantMessageDto chat(User user, String message, long conversationId);

    String getResponse(String userPrompt);

    AssistantMessageDto createGuestConversation(String message);

    PageResult<MessageDto> loadConversation(User user, long conversationId, int page, int pageSize);

    PageResult<ConversationDto> loadConversations(User user, int page, int pageSize);

    ConversationDto renameConversation(User user, long conversationId, String title);

    void deleteConversation(User user, long conversationId);
}
