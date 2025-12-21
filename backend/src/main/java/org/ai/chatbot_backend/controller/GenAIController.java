package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.AssistantMessageDto;
import org.ai.chatbot_backend.dto.ConversationDto;
import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.dto.UpdateTitleRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
    private final AuthHelper authHelper;

    @PostMapping("/chat")
    public ResponseEntity<AssistantMessageDto> startConversation(
            @RequestBody String message,
            Authentication authentication
    ) {
        try {
            User user = authHelper.getAuthenticatedUserOrNull(authentication);

            AssistantMessageDto response;

            if (user != null) {
                response = chatService.createAndSaveConversation(user, message);
            } else {
                response = chatService.createGuestConversation(message);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/chat/{conversationId}/messages")
    public ResponseEntity<AssistantMessageDto> continueConversation(
            @PathVariable long conversationId,
            @RequestBody String message,
            Authentication authentication
    ) {
        try {

            User user = authHelper.getAuthenticatedUserOrNull(authentication);

            AssistantMessageDto response;

            if (user != null) {
                 response = chatService.chat(user, message, conversationId);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/chat/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable long conversationId,
            Authentication authentication) {

        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ConversationDto conversation =
                    chatService.loadConversation(user, conversationId);

            return ResponseEntity.ok(conversation);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/chat")
    public ResponseEntity<List<ConversationDto>> getConversations(Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<ConversationDto> conversation =
                    chatService.loadConversations(user);

            return ResponseEntity.ok(conversation);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
            ConversationDto conversation = chatService.updateConversationTitle(user, conversationId, request.getTitle());

            return ResponseEntity.ok(conversation);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }


    @PostMapping("/recipes")
    public ResponseEntity<String> generateRecipe(@RequestParam String ingredients,
                                                 @RequestParam(defaultValue = "any") String cuisine,
                                                 @RequestParam(defaultValue = "") String dietaryRestrictions,
                                                 Authentication authentication) {
        try {
            CreateRecipeResult result = recipeService.createRecipe(ingredients, cuisine, dietaryRestrictions);
            User user = authHelper.getAuthenticatedUserOrNull(authentication);
            if (user != null) {
                String title = recipeService.extractRecipeTitle(result.getRecipeMarkdown());
                String contentWithoutLink = result.contentWithoutDownload().trim();
                SaveRecipeInHistoryRequest recipeHistoryRequest = new SaveRecipeInHistoryRequest();
                recipeHistoryRequest.setTitle(title);
                recipeHistoryRequest.setContent(contentWithoutLink);
                recipeHistoryRequest.setFileId(result.getFileId());
                recipeHistoryService.save(user.getId(), recipeHistoryRequest);
            }
            String fullText = result.toFullText();
            return ResponseEntity.ok(fullText);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/recipes/download/{id}")
    public ResponseEntity<Resource> downloadRecipe(@PathVariable Long id) {
        Resource resource = recipeFileService.getRecipeFile(id);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe-" + id + ".txt")
                .body(resource);
    }


    @PostMapping("/food-images")
    public ResponseEntity<String> generateFoodImage(Authentication authentication,
                                    @RequestParam(defaultValue = "") String name,
                                    @RequestParam String style,
                                    @RequestParam(defaultValue = "1024x1024") String size,
                                    @RequestParam(required = false) String course,
                                    @RequestParam(required = false) String mainIngredient,
                                    @RequestParam(required = false) String dishType) {

        if (!style.equalsIgnoreCase("vivid") && !style.equalsIgnoreCase("natural")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorry, the picked style is invalid");
        }

        if (!size.equalsIgnoreCase("1024x1024") && !size.equalsIgnoreCase("1792x1024") &&
                !size.equalsIgnoreCase("1024x1792")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sorry, the picked size is invalid");
        }

        try {
            String tempImageUrl = imageService.generateFoodImageFromParams(name, course, mainIngredient, dishType, style, size);
            if (tempImageUrl.isEmpty()) {
                throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
            }
            log.info(tempImageUrl);
            User user = authHelper.getAuthenticatedUserOrNull(authentication);
            if (user != null) {
                String publicImageUrl = imageService.persistImageForUser(tempImageUrl, user.getId());
                log.info(publicImageUrl);
                return ResponseEntity.ok(publicImageUrl);
            }
            return ResponseEntity.ok(tempImageUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
