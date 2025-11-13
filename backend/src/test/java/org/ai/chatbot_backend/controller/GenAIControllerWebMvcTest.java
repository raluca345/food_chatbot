package org.ai.chatbot_backend.controller;

import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GenAIController.class)
class GenAIControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;
    @MockitoBean
    private ImageService imageService;
    @MockitoBean
    private RecipeService recipeService;
    @MockitoBean
    private RecipeFileService recipeFileService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private RecipeHistoryService recipeHistoryService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@example.com")
    void generateRecipe_validPrompt_savesHistory() throws Exception {
        String recipeText = "**Recipe Title:** Yummy\n\nIngredients:\n- a\n- b\n\nDownload link here";

        when(recipeService.createRecipe(anyString(), anyString(), anyString())).thenReturn(recipeText);
        when(userService.findUserIdByEmail("user@example.com")).thenReturn(123L);
        when(recipeService.extractRecipeTitle(recipeText)).thenReturn("Yummy");

        mockMvc.perform(post("/api/v1/recipes")
                .with(csrf())
                .param("ingredients", "ing"))
                .andExpect(status().isOk())
                .andExpect(content().string(recipeText));

        ArgumentCaptor<SaveRecipeInHistoryRequest> captor = ArgumentCaptor.forClass(SaveRecipeInHistoryRequest.class);
        verify(recipeHistoryService, times(1)).save(eq(123L), captor.capture());

        SaveRecipeInHistoryRequest saved = captor.getValue();
        assertFalse(saved.getContent().contains("Download link here"));
        assertFalse(saved.getContent().trim().endsWith("Download link here"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void generateRecipe_inappropriatePrompt_noSave() throws Exception {
        when(recipeService.createRecipe(anyString(), anyString(), anyString()))
                .thenThrow(new InappropriateRequestRefusalException("inappropriate"));

        mockMvc.perform(post("/api/v1/recipes").with(csrf()).param("ingredients", "bad"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recipeHistoryService);
    }
}
