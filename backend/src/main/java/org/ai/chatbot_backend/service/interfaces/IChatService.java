package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.dto.AssistantMessageDto;
import org.ai.chatbot_backend.dto.ConversationDto;
import org.ai.chatbot_backend.model.User;

import java.util.List;

public interface IChatService {

    String systemPrompt();

    boolean looksLikeRecipe(String text);

    String createDownloadableRecipe(String recipeText);

    AssistantMessageDto chat(User user, String message, long conversationId);

    String getResponse(String userPrompt);

    AssistantMessageDto createGuestConversation(String message);

    ConversationDto loadConversation(User user, long conversationId);

    List<ConversationDto> loadConversations(User user);

    ConversationDto updateConversationTitle(User user, long conversationId, String title);

    void deleteConversation(User user, long conversationId);
}
