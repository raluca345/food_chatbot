package org.ai.chatbot_backend.service.implementations;

import com.azure.core.exception.HttpResponseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.AssistantMessageDto;
import org.ai.chatbot_backend.dto.ConversationDto;
import org.ai.chatbot_backend.dto.MessageDto;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.Message;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.service.interfaces.IChatService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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
        return """
                You are a helpful assistant that only answers questions about food, recipes, ingredients, and cooking. \
                Important rules: \
                1) Do NOT invent or include any download links or URLs. \
                2) Do NOT mention internal IDs (recipe id, database id, file id) or placeholders. \
                3) If you provide a recipe, output ONLY the recipe in markdown using this strict format: \
                   ### <Title>
                
                #### Ingredients:
                - ...
                
                #### Instructions:
                1. ... \
                No extra commentary before or after the recipe. \
                If the user asks to download/save/export, just acknowledge; the backend will generate and append the download link. \
                If the question is not about food, politely respond: 'Sorry, I can only answer questions about food.'""";
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
    public String createDownloadableRecipe(String recipeText, Long userId) {
        if (userId == null) {
            return "";
        }
        Long id = recipeFileService.storeRecipeText(recipeText);
        recipeFileService.attachFileToUser(id, userId);
        return recipeFileService.getDownloadMarkdown(id, backendBaseUrl);
    }

    @Transactional
    public AssistantMessageDto createAndSaveConversation(User user, String userMessage) {

        Conversation conversation =
                conversationService.createConversationWithFirstMessage(user, userMessage);
        conversation.setTitle(conversationService.createTitle(chatModel, userMessage));
        log.info(conversation.getTitle());

        String assistantReply = getResponse(userMessage, user.getId());

        messageService.createAssistantMessage(assistantReply, conversation);

        return new AssistantMessageDto(
                conversation.getId(),
                assistantReply
        );
    }

    @Override
    public AssistantMessageDto createGuestConversation(String message) {

        String reply = getResponse(message, null);

        return new AssistantMessageDto(null, reply);
    }

    @Override
    public PageResult<MessageDto> loadConversation(User user, long conversationId, int page, int pageSize) {

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Conversation conversation = conversationService.findById(conversationId);

        if (conversation.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("Conversation does not belong to user");
        }

        return messageService.getMessagesForConversationPaged(conversationId, page, pageSize);
    }

    @Override
    public PageResult<ConversationDto> loadConversations(User user, int page, int pageSize) {
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Page<Conversation> conversations = conversationService.findByUser(user, page, pageSize);
        List<ConversationDto> items = conversations
                .stream()
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
        return new PageResult<>(items, conversations.getTotalElements());
    }

    @Override
    public ConversationDto renameConversation(User user, long conversationId, String title) {

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

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Conversation conversation = conversationService.findById(conversationId);

        if (conversation.getUser() == null || conversation.getUser().getId() != user.getId()) {
            throw new AccessDeniedException("Conversation does not belong to user");
        }

        String assistantReply = getResponse(userMessage, user.getId());

        messageService.createUserMessage(userMessage, conversation);
        messageService.createAssistantMessage(assistantReply, conversation);

        return new AssistantMessageDto(
                conversation.getId(),
                assistantReply
        );
    }

    private String getResponse(String userPrompt, Long userId) {
        String fullPrompt = systemPrompt() + "\nUser: " + userPrompt;
        try {
            String modelOut = chatModel.call(fullPrompt);
            if (looksLikeRecipe(modelOut)) {
                String mdLink = createDownloadableRecipe(modelOut, userId);
                if (!mdLink.isBlank()) {
                    modelOut = modelOut + "\n\nYou can download this recipe here: " + mdLink;
                }
            }
            return modelOut;
        } catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }

    @Override
    public String getResponse(String userPrompt) {
        return getResponse(userPrompt, null);
    }

}
