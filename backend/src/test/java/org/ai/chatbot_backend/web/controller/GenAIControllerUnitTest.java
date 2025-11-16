package org.ai.chatbot_backend.web.controller;

import org.ai.chatbot_backend.controller.GenAIController;
import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    private UserService userService;
    @Mock
    private RecipeHistoryService recipeHistoryService;

    @InjectMocks
    private GenAIController genAIController;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
        // mark these stubbings as lenient so tests that exercise error paths don't fail with unnecessary stubbing
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getName()).thenReturn("user@example.com");
    }

    @Test
    void generateRecipe_validPrompt_savesHistory() {
        String recipeText = "**Recipe Title:** Yummy\n\nIngredients:\n- a\n- b\n\nDownload link here";

        CreateRecipeResult createResult = new CreateRecipeResult(recipeText, 42L, "[Download recipe](http://localhost/api/v1/recipes/download/42)");

        when(recipeService.createRecipe(anyString(), anyString(), anyString())).thenReturn(createResult);
        when(userService.findUserIdByEmail("user@example.com")).thenReturn(123L);
        when(recipeService.extractRecipeTitle(recipeText)).thenReturn("Yummy");

        var resp = genAIController.generateRecipe("ing", "any", "", auth);

        assertEquals(HttpStatusCode.valueOf(200), resp.getStatusCode());
        assertEquals(createResult.toFullText(), resp.getBody());

        ArgumentCaptor<SaveRecipeInHistoryRequest> captor = ArgumentCaptor.forClass(SaveRecipeInHistoryRequest.class);
        verify(recipeHistoryService, times(1)).save(eq(123L), captor.capture());

        SaveRecipeInHistoryRequest saved = captor.getValue();
        assertEquals("Yummy", saved.getTitle());
        assertTrue(saved.getContent().contains("Ingredients"));
        assertFalse(saved.getContent().contains("Download link here"));
        assertFalse(saved.getContent().trim().endsWith("Download link here"));
        assertEquals(42L, saved.getFileId());
    }

    @Test
    void generateRecipe_inappropriatePrompt_noSaveAndReturnsBadRequest() {
        when(recipeService.createRecipe(anyString(), anyString(), anyString()))
                .thenThrow(new InappropriateRequestRefusalException("inappropriate"));

        var resp = genAIController.generateRecipe("bad", "any", "", auth);

        assertEquals(HttpStatusCode.valueOf(400), resp.getStatusCode());
        assertEquals("inappropriate", resp.getBody());

        verifyNoInteractions(recipeHistoryService);
    }
}
