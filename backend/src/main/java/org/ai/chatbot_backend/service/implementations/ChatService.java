package org.ai.chatbot_backend.service.implementations;

import com.openai.errors.OpenAIException;
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
                You are a helpful assistant that answers questions about food, recipes, ingredients, and cooking.

                IMPORTANT INSTRUCTIONS:

                1) ANSWER SIMPLE FOOD QUESTIONS DIRECTLY:
                   - "What is pizza?" → Answer what pizza is, its history, variations, etc.
                   - "What are the benefits of tomatoes?" → Answer about nutritional benefits.
                   - "Can cats eat pizza?" → Answer yes/no with brief explanation.
                   DO NOT turn these into recipes unless explicitly asked.

                2) ONLY GENERATE FULL RECIPES when:
                   - User explicitly asks "How do I make..." or "Recipe for..."
                   - User asks "What is [food]?" AND you choose to provide the recipe format
                   When providing a recipe, use ONLY this strict markdown format with NO extra text before or after:
                   ### <Title>

                   #### Ingredients:
                   - ingredient 1
                   - ingredient 2

                   #### Instructions:
                   1. step 1
                   2. step 2

                3) REFUSALS:
                   - If question is NOT about food: respond only with "Sorry, I can only answer questions about food."
                   - Do NOT invent or include any download links or URLs
                   - Do NOT mention internal IDs or database references

                4) DOWNLOAD LINKS:
                   - Backend will append download link automatically if you provide a recipe
                   - Do NOT add it yourself""";
        }

    @Override
    public boolean looksLikeRecipe(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String lower = text.toLowerCase();
        if (lower.contains("sorry") ||
                lower.contains("i cannot") ||
                lower.contains("unable to") ||
                lower.contains("illegal") ||
                lower.contains("inappropriate") ||
                lower.startsWith("i can only answer")) {
            return false;
        }

        if (RECIPE_PATTERN.matcher(text).find()) {
            return true;
        }

        boolean hasTitle = text.trim().startsWith("###");
        boolean hasIngredientsSection = lower.contains("#### ingredients:");
        boolean hasInstructionsSection = lower.contains("#### instructions:");

        return hasTitle && hasIngredientsSection && hasInstructionsSection;
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
        } catch (OpenAIException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }

    @Override
    public String getResponse(String userPrompt) {
        return getResponse(userPrompt, null);
    }

}
