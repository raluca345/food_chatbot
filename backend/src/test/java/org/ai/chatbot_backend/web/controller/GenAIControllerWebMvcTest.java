package org.ai.chatbot_backend.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.controller.GenAIController;
import org.ai.chatbot_backend.dto.AssistantMessageDto;
import org.ai.chatbot_backend.dto.ConversationDto;
import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.SaveRecipeInHistoryRequest;
import org.ai.chatbot_backend.dto.UpdateTitleRequest;
import org.ai.chatbot_backend.exception.EmptyTitleException;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private ConversationService conversationService;
    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AuthHelper authHelper;

    private final ObjectMapper mapper = new ObjectMapper();

    private User makeUser(long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    @BeforeEach
    void setupAuthHelperDefault() {
        // Default: derive User from Authentication.getName(); id is 1 for simplicity
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class)))
                .thenAnswer(inv -> {
                    Authentication a = inv.getArgument(0);
                    if (a == null) return null;
                    String name = a.getName();
                    if (name == null) return null;
                    return makeUser(1L, name);
                });
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void generateRecipe_validPrompt_savesHistory() throws Exception {
        String recipeText = "**Recipe Title:** Yummy\n\nIngredients:\n- a\n- b\n\nDownload link here";

        CreateRecipeResult createResult = new CreateRecipeResult(
                recipeText, 42L, "[Download recipe](http://localhost/api/v1/recipes/download/42)");

        when(recipeService.createRecipe(anyString(), anyString(), anyString())).thenReturn(createResult);
        when(recipeService.extractRecipeTitle(recipeText)).thenReturn("Yummy");

        mockMvc.perform(post("/api/v1/recipes")
                .with(csrf())
                .param("ingredients", "ing"))
                .andExpect(status().isOk())
                .andExpect(content().string(createResult.toFullText()));

        ArgumentCaptor<SaveRecipeInHistoryRequest> requestCaptor = ArgumentCaptor.forClass(SaveRecipeInHistoryRequest.class);
        verify(recipeHistoryService, times(1)).save(anyLong(), requestCaptor.capture());

        SaveRecipeInHistoryRequest saved = requestCaptor.getValue();
        assertFalse(saved.getContent().contains("Download link here"));
        assertFalse(saved.getContent().trim().endsWith("Download link here"));
        assertEquals(42L, saved.getFileId());
        assertEquals("Yummy", saved.getTitle());
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

    @Test
    @WithMockUser(username = "user@example.com")
    void createConversation_ValidPromptSameUser_savesHistory() throws Exception {
        when(chatService.createAndSaveConversation(any(User.class), anyString()))
                .thenReturn(new AssistantMessageDto(1L, "hi"));

        String promptJson = mapper.writeValueAsString("hello");

        mockMvc.perform(post("/api/v1/chat")
            .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(promptJson))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void continueConversation_RightUser_savesHistory() throws Exception {
        when(chatService.chat(any(User.class), anyString(), anyLong()))
                .thenReturn(new AssistantMessageDto(1L, "hello"));

        String promptJson = mapper.writeValueAsString("how are you");

        mockMvc.perform(post("/api/v1/chat/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(promptJson))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user2@example.com")
    void continueConversation_WrongUser_returnsUnauthorized() throws Exception {
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))).thenReturn(null);

        String promptJson = mapper.writeValueAsString("how are you");

        mockMvc.perform(post("/api/v1/chat/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(promptJson))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(chatService);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void continueConversation_RightUser_ConvoNotFound_returnsBadRequest() throws Exception {
        when(chatService.chat(any(User.class), anyString(), anyLong()))
                .thenThrow(new ResourceNotFoundException("no convo with that id"));

        String promptJson = mapper.writeValueAsString("how are you");

        mockMvc.perform(post("/api/v1/chat/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(promptJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageService);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getConversation_RightUser_returnsConversation() throws Exception {
        ConversationDto dto = new ConversationDto(1L, "title", List.of());
        when(chatService.loadConversation(any(User.class), anyLong())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/chat/1").with(csrf()))
                .andExpect(status().isOk());
        verify(chatService, times(1)).loadConversation(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getConversation_NotFound_returns404() throws Exception {
        when(chatService.loadConversation(any(User.class), anyLong()))
                .thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/chat/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getConversation_AccessDenied_returns403() throws Exception {
        when(chatService.loadConversation(any(User.class), anyLong()))
                .thenThrow(new AccessDeniedException("forbidden"));

        mockMvc.perform(get("/api/v1/chat/2").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user2@example.com")
    void getConversation_Unauthorized_returns401() throws Exception {
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))).thenReturn(null);

        mockMvc.perform(get("/api/v1/chat/1").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chatService);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void listConversations_RightUser_returnsList() throws Exception {
        List<ConversationDto> list = List.of(
                new ConversationDto(1L, "t1", List.of()),
                new ConversationDto(2L, "t2", List.of())
        );
        when(chatService.loadConversations(any(User.class))).thenReturn(list);

        mockMvc.perform(get("/api/v1/chat").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void listConversations_NotFound_returns404() throws Exception {
        when(chatService.loadConversations(any(User.class)))
                .thenThrow(new ResourceNotFoundException("none"));

        mockMvc.perform(get("/api/v1/chat").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void listConversations_AccessDenied_returns403() throws Exception {
        when(chatService.loadConversations(any(User.class)))
                .thenThrow(new AccessDeniedException("forbidden"));

        mockMvc.perform(get("/api/v1/chat").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user2@example.com")
    void listConversations_Unauthorized_returns401() throws Exception {
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))).thenReturn(null);
        mockMvc.perform(get("/api/v1/chat").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chatService);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void updateConversationTitle_RightUser_returnsUpdated() throws Exception {
        UpdateTitleRequest req = new UpdateTitleRequest();
        req.setTitle("New Title");
        ConversationDto dto = new ConversationDto(1L, "New Title", List.of());
        when(chatService.updateConversationTitle(any(User.class), eq(1L), eq("New Title")))
                .thenReturn(dto);

        mockMvc.perform(patch("/api/v1/chat/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void updateConversationTitle_Empty_returns400() throws Exception {
        UpdateTitleRequest req = new UpdateTitleRequest();
        req.setTitle("");
        when(chatService.updateConversationTitle(any(User.class), anyLong(), anyString()))
                .thenThrow(new EmptyTitleException("empty"));

        mockMvc.perform(patch("/api/v1/chat/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void updateConversationTitle_NotFound_returns404() throws Exception {
        UpdateTitleRequest req = new UpdateTitleRequest();
        req.setTitle("X");
        when(chatService.updateConversationTitle(any(User.class), anyLong(), anyString()))
                .thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(patch("/api/v1/chat/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void updateConversationTitle_Forbidden_returns403() throws Exception {
        UpdateTitleRequest req = new UpdateTitleRequest();
        req.setTitle("X");
        when(chatService.updateConversationTitle(any(User.class), anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("no"));

        mockMvc.perform(patch("/api/v1/chat/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user2@example.com")
    void updateConversationTitle_Unauthorized_returns401() throws Exception {
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))). thenReturn(null);
        UpdateTitleRequest req = new UpdateTitleRequest();
        req.setTitle("X");

        mockMvc.perform(patch("/api/v1/chat/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chatService);
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void deleteConversation_RightUser_returns204() throws Exception {
        doNothing().when(chatService).deleteConversation(any(User.class), eq(3L));

        mockMvc.perform(delete("/api/v1/chat/3").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void deleteConversation_NotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("missing")).when(chatService)
                .deleteConversation(any(User.class), anyLong());

        mockMvc.perform(delete("/api/v1/chat/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void deleteConversation_Forbidden_returns403() throws Exception {
        doThrow(new AccessDeniedException("forbidden")).when(chatService)
                .deleteConversation(any(User.class), anyLong());

        mockMvc.perform(delete("/api/v1/chat/2").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user2@example.com")
    void deleteConversation_Unauthorized_returns401() throws Exception {
        when(authHelper.getAuthenticatedUserOrNull(any(Authentication.class))).thenReturn(null);
        mockMvc.perform(delete("/api/v1/chat/2").with(csrf()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chatService);
    }
}
