package org.ai.chatbot_backend.web.controller;

import org.ai.chatbot_backend.controller.GenAIController;
import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.RecipeRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.ChatService;
import org.ai.chatbot_backend.service.implementations.ImageService;
import org.ai.chatbot_backend.service.implementations.RecipeFileService;
import org.ai.chatbot_backend.service.implementations.RecipeHistoryService;
import org.ai.chatbot_backend.service.implementations.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAIControllerUnitTest {

    @Mock
    private ChatService chatService;
    @Mock
    private ImageService imageService;
    @Mock
    private RecipeService recipeService;
    @Mock
    private RecipeFileService recipeFileService;
    @Mock
    private RecipeHistoryService recipeHistoryService;
    @Mock
    private AuthHelper authHelper;

    @InjectMocks
    private GenAIController genAIController;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        auth = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId(123L);
        user.setEmail("user@example.com");
        org.mockito.Mockito.lenient().when(authHelper.getAuthenticatedUserOrNull(auth)).thenReturn(user);
    }

    @Test
    void generateRecipe_validPrompt_savesHistory() {
        String recipeText = "**Recipe Title:** Yummy\n\nIngredients:\n- a\n- b\n\nDownload link here";

        CreateRecipeResult createResult = new CreateRecipeResult(
                recipeText,
                42L,
                "[Download recipe](http://localhost/api/v1/recipes/download/42)"
        );

        RecipeRequest request = new RecipeRequest();
        request.setIngredients("ing");
        request.setCuisine("any");
        request.setDietaryRestrictions("");

        when(recipeService.createRecipe(any(RecipeRequest.class))).thenReturn(createResult);

        var resp = genAIController.generateRecipe(request, auth);

        assertEquals(HttpStatusCode.valueOf(200), resp.getStatusCode());
        CreateRecipeResult responseBody = (CreateRecipeResult) resp.getBody();
        assertEquals(createResult.getRecipeMarkdown(), responseBody.getRecipeMarkdown());
        assertEquals(createResult.getFileId(), responseBody.getFileId());
        assertEquals(createResult.getDownloadMarkdown(), responseBody.getDownloadMarkdown());
        assertEquals(createResult.toFullText(), responseBody.getFullText());
        verify(recipeHistoryService).saveGeneratedRecipe(eq(123L), eq(createResult));
    }

    @Test
    void generateRecipe_inappropriatePrompt_noSaveAndReturnsBadRequest() {
        RecipeRequest request = new RecipeRequest();
        request.setIngredients("bad");
        request.setCuisine("any");
        request.setDietaryRestrictions("");

        when(recipeService.createRecipe(any(RecipeRequest.class)))
                .thenThrow(new InappropriateRequestRefusalException("inappropriate"));

        var resp = genAIController.generateRecipe(request, auth);

        assertEquals(HttpStatusCode.valueOf(400), resp.getStatusCode());
        assertEquals("inappropriate", resp.getBody());

        verifyNoInteractions(recipeHistoryService);
    }
}
