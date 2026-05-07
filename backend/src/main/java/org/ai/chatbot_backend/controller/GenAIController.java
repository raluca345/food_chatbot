package org.ai.chatbot_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.*;
import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(
        name = "1. AI & Conversations",
        description = "Chat, recipe generation, image generation and related downloads")
public class GenAIController {
    private final ChatService chatService;
    private final ImageService imageService;
    private final RecipeService recipeService;
    private final RecipeFileService recipeFileService;
    private final RecipeHistoryService recipeHistoryService;
    private final ConversationService conversationService;
    private final AuthHelper authHelper;

    @Operation(
            summary = "Start conversation (authenticated)",
            description =
                    "Creates conversation for the authenticated user and returns the assistant response.",
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageRequest.class),
                            examples =
                            @ExampleObject(
                                    name = "Start conversation request",
                                    value =
                                            """
                                                    {
                                                      "message": "What can I cook with eggs and cheese?"
                                                    }
                                                    """))))
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Conversation created successfully",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssistantMessageDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            })
    @PostMapping("/chat")
    public ResponseEntity<AssistantMessageDto> startConversation(
            @RequestBody ChatMessageRequest request, Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AssistantMessageDto response =
                chatService.createAndSaveConversation(user, request.getMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Start guest conversation",
            description =
                    "Generates an assistant response for a guest user without saving conversation data.",
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageRequest.class),
                            examples =
                            @ExampleObject(
                                    name = "Guest chat request",
                                    value =
                                            """
                                                    {
                                                      "message": "Suggest a quick pasta recipe."
                                                    }
                                                    """))))
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Response generated successfully",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssistantMessageDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            })
    @PostMapping("/chat/guest")
    public ResponseEntity<AssistantMessageDto> startGuestConversation(
            @RequestBody ChatMessageRequest request) {
        AssistantMessageDto response = chatService.createGuestConversation(request.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Continue conversation",
            description =
                    "Adds a user message to an existing conversation and returns the assistant response.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Assistant response returned successfully",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssistantMessageDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Conversation not found")
            })
    @PostMapping("/chat/{conversationId}/messages")
    public ResponseEntity<AssistantMessageDto> continueConversation(
            @Parameter(description = "Conversation id", example = "42") @PathVariable long conversationId,
            @RequestBody ChatMessageRequest request,
            Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AssistantMessageDto response = chatService.chat(user, request.getMessage(), conversationId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get conversation",
            description =
                    "Retrieves a conversation by ID, with its title and a paginated message list ordered by newest first.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Conversation returned",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ConversationDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Conversation not found")
            })
    @GetMapping("/chat/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(
            @Parameter(description = "Conversation id", example = "42") @PathVariable long conversationId,
            @Parameter(description = "1-based page number", example = "1")
            @RequestParam(defaultValue = "1")
            int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20")
            int pageSize,
            Authentication authentication) {

        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PageResult<MessageDto> conversation =
                chatService.loadConversation(user, conversationId, page, pageSize);
        Conversation conversationEntity = conversationService.findById(conversationId);
        ConversationDto body =
                new ConversationDto(
                        conversationId,
                        conversationEntity.getTitle(),
                        conversation.getItems(),
                        conversation.getTotal());
        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Get conversations",
            description =
                    "Returns all conversations for the authenticated user as a paginated list, ordered by most "
                            + "recently updated first.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Conversations returned",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PageResult.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping("/chat")
    public ResponseEntity<PageResult<ConversationDto>> getConversations(
            Authentication authentication,
            @Parameter(description = "1-based page number", example = "1")
            @RequestParam(defaultValue = "1")
            int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20")
            int pageSize) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PageResult<ConversationDto> conversation = chatService.loadConversations(user, page, pageSize);
        return ResponseEntity.ok(conversation);
    }

    @Operation(
            summary = "Rename conversation",
            description = "Updates the title of one conversation.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Conversation renamed",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ConversationDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Conversation not found")
            })
    @PatchMapping(
            path = "/chat/{conversationId}",
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<ConversationDto> updateConversationTitle(
            @Parameter(description = "Conversation id", example = "42") @PathVariable long conversationId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateTitleRequest.class),
                            examples =
                            @ExampleObject(
                                    name = "Rename conversation request",
                                    value =
                                            """
                                                    {
                                                      "title": "Weeknight Pasta Ideas"
                                                    }
                                                    """)))
            @RequestBody
            UpdateTitleRequest request,
            Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ConversationDto conversation =
                chatService.renameConversation(user, conversationId, request.getTitle());
        return ResponseEntity.ok(conversation);
    }

    @Operation(
            summary = "Delete conversation",
            description = "Deletes a conversation and its messages for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "204", description = "Conversation deleted"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Conversation not found")
            })
    @DeleteMapping("/chat/{conversationId}")
    public ResponseEntity<String> deleteConversation(
            @Parameter(description = "Conversation id", example = "42") @PathVariable long conversationId,
            Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        chatService.deleteConversation(user, conversationId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Conversation has been deleted");
    }

    @Operation(
            summary = "Generate recipe",
            description =
                    "Generates a recipe based on provided ingredients, cuisine and dietary preferences. Works for"
                            + " both guest and authenticated users. Authenticated users have their recipes saved to history.",
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RecipeRequest.class),
                            examples =
                            @ExampleObject(
                                    name = "Recipe generation request example",
                                    value =
                                            """
                                                    {
                                                      "ingredients": "chicken breast, tomatoes, garlic, basil, olive oil",
                                                      "cuisine": "mediterranean",
                                                      "dietaryRestrictions": "high-protein, low-carb"
                                                    }
                                                    """))))
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Recipe generated successfully",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = CreateRecipeResult.class))
                            }),
                    @ApiResponse(responseCode = "400", description = "Invalid ingredients or request"),
                    @ApiResponse(responseCode = "404", description = "Recipe generation failed")
            })
    @PostMapping("/recipes")
    public ResponseEntity<?> generateRecipe(
            @RequestBody RecipeRequest request, Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        CreateRecipeResult result =
                recipeService.createRecipe(request, user != null ? user.getId() : null);
        if (user != null) {
            recipeHistoryService.saveGeneratedRecipe(user.getId(), result);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Download recipe file",
            description = "Downloads a recipe file by its id. Requires authentication.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Recipe downloaded"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "404", description = "Recipe file not found")
            })
    @GetMapping("/recipes/download/{id}")
    public ResponseEntity<Resource> downloadRecipe(
            @Parameter(description = "Recipe file id", example = "214") @PathVariable Long id,
            Authentication authentication) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Resource resource = recipeFileService.getRecipeFileForUser(id, user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe-" + id + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @Operation(
            summary = "Download guest recipe",
            description = "Builds a temporary downloadable text file from the given guest recipe (in markdown format).")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Recipe downloaded"),
                    @ApiResponse(responseCode = "400", description = "Missing or invalid recipe")
            })
    @PostMapping("/recipes/download/guest")
    public ResponseEntity<Resource> downloadGuestRecipe(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RecipeDownloadRequest.class),
                            examples =
                            @ExampleObject(
                                    name = "Guest recipe download request",
                                    value =
                                            """
                                                    {
                                                      "recipeMarkdown": "## Ingredients\\n- eggs\\n- cheese\\n\\n## Instructions\\n1. Mix\\n2. Bake"
                                                    }
                                                    """)))
            @RequestBody
            RecipeDownloadRequest request) {
        if (request == null
                || request.getRecipeMarkdown() == null
                || request.getRecipeMarkdown().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = recipeFileService.getRecipeFileForGuest(request.getRecipeMarkdown());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @Operation(
            summary = "Generate food image",
            description =
                    "Generates a food image URL from the given parameters. Authenticated users also persist the image" +
                            " to gallery.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Image generated successfully",
                            content =
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ImageDto.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Image generation failed")
            })
    @PostMapping("/food-images")
    public ResponseEntity<?> generateFoodImage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FoodImageRequest.class),
                            examples =
                            @ExampleObject(
                                    name = "Food image generation request",
                                    value =
                                            """
                                                    {
                                                      "name": "Margherita Pizza",
                                                      "style": "natural",
                                                      "size": "1024x1024",
                                                      "course": "main",
                                                      "ingredients": "tomato, mozzarella, basil",
                                                      "dishType": "pizza"
                                                    }
                                                    """)))
            @RequestBody
            FoodImageRequest request,
            Authentication authentication)
            throws Exception {
        String tempImageUrl = imageService.generateFoodImageFromParams(request);
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user != null) {
            ImageDto generatedImage = imageService.persistImageForUser(tempImageUrl, user.getId());
            return ResponseEntity.ok(generatedImage);
        }
        return ResponseEntity.ok(new ImageDto(0L, tempImageUrl, null, null));
    }
}
