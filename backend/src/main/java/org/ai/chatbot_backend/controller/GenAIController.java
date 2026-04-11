package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.AssistantMessageDto;
import org.ai.chatbot_backend.dto.ChatMessageRequest;
import org.ai.chatbot_backend.dto.ConversationDto;
import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.FoodImageRequest;
import org.ai.chatbot_backend.dto.ImageDto;
import org.ai.chatbot_backend.dto.MessageDto;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.dto.RecipeDownloadRequest;
import org.ai.chatbot_backend.dto.RecipeRequest;
import org.ai.chatbot_backend.dto.UpdateTitleRequest;
import org.ai.chatbot_backend.exception.EmptyTitleException;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
public class GenAIController {
    private final ChatService chatService;
    private final ImageService imageService;
    private final RecipeService recipeService;
    private final RecipeFileService recipeFileService;
    private final RecipeHistoryService recipeHistoryService;
    private final ConversationService conversationService;
    private final AuthHelper authHelper;

    @PostMapping("/chat")
    public ResponseEntity<AssistantMessageDto> startConversation(
            @RequestBody ChatMessageRequest request,
            Authentication authentication
    ) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AssistantMessageDto response = chatService.createAndSaveConversation(user, request.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/chat/guest")
    public ResponseEntity<AssistantMessageDto> startGuestConversation(
            @RequestBody ChatMessageRequest request
    ) {
        try {
            AssistantMessageDto response = chatService.createGuestConversation(request.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/chat/{conversationId}/messages")
    public ResponseEntity<AssistantMessageDto> continueConversation(
            @PathVariable long conversationId,
            @RequestBody ChatMessageRequest request,
            Authentication authentication
    ) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AssistantMessageDto response = chatService.chat(user, request.getMessage(), conversationId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (InappropriateRequestRefusalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/chat/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable long conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {

        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            PageResult<MessageDto> conversation = chatService.loadConversation(user, conversationId, page, pageSize);
            Conversation conversationEntity = conversationService.findById(conversationId);
            ConversationDto body = new ConversationDto(
                    conversationId,
                    conversationEntity.getTitle(),
                    conversation.getItems(),
                    conversation.getTotal()
            );
            return ResponseEntity.ok(body);

        } catch (ResourceNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        }
    }

    @GetMapping("/chat")
    public ResponseEntity<PageResult<ConversationDto>> getConversations(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            PageResult<ConversationDto> conversation =
                    chatService.loadConversations(user, page, pageSize);

            return ResponseEntity.ok(conversation);

        } catch (ResourceNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        }
    }

    @PatchMapping(path = "/chat/{conversationId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ConversationDto> updateConversationTitle(@PathVariable long conversationId,
                                                                   @RequestBody UpdateTitleRequest request,
                                                                    Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ConversationDto conversation = chatService.renameConversation(user, conversationId, request.getTitle());

            return ResponseEntity.ok(conversation);
        } catch (ResourceNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (EmptyTitleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    @DeleteMapping("/chat/{conversationId}")
    public ResponseEntity<String> deleteConversation(@PathVariable long conversationId,
                                                     Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            chatService.deleteConversation(user, conversationId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Conversation has been deleted");
        } catch (ResourceNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @PostMapping("/recipes")
    public ResponseEntity<?> generateRecipe(@RequestBody RecipeRequest request,
                                            Authentication authentication) {
        try {
            User user = authHelper.getAuthenticatedUserOrNull(authentication);
            CreateRecipeResult result = recipeService.createRecipe(request, user != null ? user.getId() : null);
            if (user != null) {
                recipeHistoryService.saveGeneratedRecipe(user.getId(), result);
            }
            return ResponseEntity.ok(result);
        } catch (InappropriateRequestRefusalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Recipe generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @GetMapping("/recipes/download/{id}")
    public ResponseEntity<Resource> downloadRecipe(@PathVariable Long id, Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Resource resource = recipeFileService.getRecipeFileForUser(id, user.getId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe-" + id + ".txt")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/recipes/download/guest")
    public ResponseEntity<Resource> downloadGuestRecipe(@RequestBody RecipeDownloadRequest request) {
        if (request == null || request.getRecipeMarkdown() == null || request.getRecipeMarkdown().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = recipeFileService.getRecipeFileForGuest(request.getRecipeMarkdown());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }


    @PostMapping("/food-images")
    public ResponseEntity<?> generateFoodImage(
            @RequestBody FoodImageRequest request,
            Authentication authentication
    ) {
        try {
            String tempImageUrl = imageService.generateFoodImageFromParams(request);
            if (tempImageUrl.isEmpty()) {
                throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
            }
            log.info(tempImageUrl);
            User user = authHelper.getAuthenticatedUserOrNull(authentication);
            if (user != null) {
                ImageDto generatedImage = imageService.persistImageForUser(tempImageUrl, user.getId());
                log.info(generatedImage.getUrl());
                return ResponseEntity.ok(generatedImage);
            }
            return ResponseEntity.ok(new ImageDto(0L, tempImageUrl, null, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
