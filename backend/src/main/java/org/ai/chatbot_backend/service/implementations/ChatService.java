package org.ai.chatbot_backend.service.implementations;

import com.azure.core.exception.HttpResponseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.AssistantMessageDto;
import org.ai.chatbot_backend.dto.ConversationDto;
import org.ai.chatbot_backend.dto.MessageDto;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.Message;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.ConversationRepository;
import org.ai.chatbot_backend.repository.UserRepository;
import org.ai.chatbot_backend.service.interfaces.IChatService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService implements IChatService {
    private final ChatModel chatModel;
    private final RecipeFileService recipeFileService;
    private final ConversationService conversationService;
    private final MessageService messageService;

    private static final Pattern RECIPE_PATTERN = Pattern.compile(
            "(?s)^###\\s+.+?\\R+####\\s+Ingredients:.*?####\\s+Instructions:",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    @Override
    public String systemPrompt() {
        return "You are a helpful assistant that only answers questions about food, recipes, ingredients, and cooking. " +
                "If the user asks to download a recipe, always use the backend API base URL: [Download Recipe]("
                + backendBaseUrl + "/api/v1/recipes/download/{recipeId}). " +
                "Never use the frontend domain. " +
                "If the question is not about food, politely respond: 'Sorry, I can only answer questions about food.'";
    }

    @Override
    public boolean looksLikeRecipe(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        if (text.toLowerCase().contains("i cannot") ||
                text.toLowerCase().contains("i'm sorry") ||
                text.toLowerCase().contains("unable to") ||
                text.toLowerCase().contains("clarify") ||
                text.toLowerCase().contains("illegal") ||
                text.toLowerCase().contains("inappropriate")) {
            return false;
        }

        if (RECIPE_PATTERN.matcher(text).find()) {
            return true;
        }

        String lower = text.toLowerCase();
        boolean hasTitle = text.trim().startsWith("###");
        boolean hasIngredients = lower.contains("ingredients");
        boolean hasInstructions = lower.contains("instructions");

        return hasTitle && hasIngredients && hasInstructions;
    }

    @Override
    public String createDownloadableRecipe(String recipeText) {
        Long id = recipeFileService.storeRecipeText(recipeText);
        return recipeFileService.getDownloadMarkdown(id, backendBaseUrl);
    }

    @Transactional
    public AssistantMessageDto createAndSaveConversation(User user, String userMessage) {

        Conversation conversation =
                conversationService.createConversationWithFirstMessage(user, userMessage);
        conversation.setTitle(conversationService.createTitle(chatModel, userMessage));
        log.info(conversation.getTitle());

        String assistantReply = getResponse(userMessage);

        messageService.createAssistantMessage(assistantReply, conversation);

        return new AssistantMessageDto(
                conversation.getId(),
                assistantReply
        );
    }

    @Override
    public AssistantMessageDto createGuestConversation(String message) {

        String reply = getResponse(message);

        return new AssistantMessageDto(null, reply);
    }

    @Override
    public ConversationDto loadConversation(User user, long conversationId) {

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Conversation conversation = conversationService.findById(conversationId);

        if (conversation.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("Conversation does not belong to user");
        }

        List<MessageDto> messageDtos = conversation.getMessages()
                .stream()
                .sorted(Comparator.comparing(Message::getTimestamp))
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getRole().name(),
                        m.getContent(),
                        m.getTimestamp()
                ))
                .toList();

        return new ConversationDto(
                conversation.getId(),
                conversation.getTitle(),
                messageDtos
        );
    }

    @Override
    public List<ConversationDto> loadConversations(User user) {
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        List<Conversation> conversations = conversationService.findByUser(user);

        return conversations
                .stream()
                .sorted(Comparator.comparing(Conversation::getUpdatedAt, Comparator.reverseOrder()))
                .map(c -> new ConversationDto(
                        c.getId(),
                        c.getTitle(),
                        c.getMessages().stream().map(
                                m -> new MessageDto(
                                        m.getId(),
                                        m.getRole().name(),
                                        m.getContent(),
                                        m.getTimestamp()
                                )
                        ).toList()
                        )
                ).toList();
    }

    @Override
    public ConversationDto updateConversationTitle(User user, long conversationId, String title) {

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Conversation updatedConversation = conversationService.updateTitle(user, conversationId, title);

        List<MessageDto> messageDtos = updatedConversation.getMessages()
                .stream()
                .sorted(Comparator.comparing(Message::getTimestamp))
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getRole().name(),
                        m.getContent(),
                        m.getTimestamp()
                ))
                .toList();

        return new ConversationDto(
                updatedConversation.getId(),
                updatedConversation.getTitle(),
                messageDtos
        );
    }

    @Override
    public void deleteConversation(User user, long conversationId) {
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        conversationService.deleteConversation(user, conversationId);
    }

    @Override
    public AssistantMessageDto chat(User user, String userMessage, long conversationId) {

        Conversation conversation = conversationService.findById(conversationId);

        String assistantReply = getResponse(userMessage);

        messageService.createAssistantMessage(assistantReply, conversation);

        return new AssistantMessageDto(
                conversation.getId(),
                assistantReply
        );
    }


    @Override
    public String getResponse(String userPrompt) {
        String fullPrompt = systemPrompt() + "\nUser: " + userPrompt;
        try {
            String modelOut = chatModel.call(fullPrompt);
            if (looksLikeRecipe(modelOut)) {
                String mdLink = createDownloadableRecipe(modelOut);
                modelOut = modelOut + "\n\nYou can download this recipe here: " + mdLink;
            }
            return modelOut;
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }

}
