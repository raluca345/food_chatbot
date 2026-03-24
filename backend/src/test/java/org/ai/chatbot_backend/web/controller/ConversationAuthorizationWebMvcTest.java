package org.ai.chatbot_backend.web.controller;

import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.controller.GenAIController;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GenAIController.class)
class ConversationAuthorizationWebMvcTest {

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
    private RecipeHistoryService recipeHistoryService;

    @MockitoBean
    private AuthHelper authHelper;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void patchRenameConversation_whenConversationNotOwned_thenForbidden() throws Exception {
        long conversationId = 10L;

        User user = new User();
        user.setId(1L);

        when(authHelper.getAuthenticatedUserOrNull(any())).thenReturn(user);
        when(chatService.renameConversation(eq(user), eq(conversationId), eq("new title")))
                .thenThrow(new AccessDeniedException("Conversation does not belong to user"));

        mockMvc.perform(
                        patch("/api/v1/chat/{conversationId}", conversationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"new title\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
        void patchRenameConversation_whenUnauthenticated_thenForbidden() throws Exception {
        long conversationId = 10L;

        when(authHelper.getAuthenticatedUserOrNull(any())).thenReturn(null);

        mockMvc.perform(
                        patch("/api/v1/chat/{conversationId}", conversationId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"new title\"}")
                )
                .andExpect(status().isForbidden());
    }
}
