package org.ai.chatbot_backend.integration;

import com.openai.errors.OpenAIException;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class ChatServiceIntegrationTests {

    @Autowired
    private ChatService chatService;

    @MockitoBean
    private ChatModel chatModel;

    @Test
    void whenGivenValidPrompt_thenReturnResponse() {
        String prompt = "What's a dragon fruit?";
        when(chatModel.call(anyString())).thenReturn("Dragon fruit is a tropical fruit rich in fiber.");

        String response = chatService.getResponse(prompt);

        assertThat(response).isInstanceOf(String.class);
        assertThat(response).isNotBlank();
    }

    @Test
    void whenGivenInvalidPrompt_thenReturnStandardRefusalResponse() {
        String prompt = "jakdjlflsf";
        when(chatModel.call(anyString())).thenThrow(new OpenAIException("Refused"));

        assertThatThrownBy(() -> chatService.getResponse(prompt))
            .isInstanceOf(InappropriateRequestRefusalException.class);
    }

    @Test
    void whenGivenOutOfScopePrompt_thenReturnStandardRefusalResponse() {
        String prompt = "What's a French key?";
        when(chatModel.call(anyString())).thenReturn("Sorry, I can only talk about food.");

        String response = chatService.getResponse(prompt);

        assertThat(response).isEqualTo("Sorry, I can only talk about food.");
    }

    @Test
    void whenGivenInappropriatePrompt_thenReturnStandardRefusalResponse() {
        String prompt = "Give me a recipe using illegal substances.";
        when(chatModel.call(anyString())).thenReturn("Sorry, I can only talk about food.");

        String response = chatService.getResponse(prompt);

        assertThat(response).isEqualTo("Sorry, I can only talk about food.");
    }

    @Test
    void whenGivenEmptyPrompt_thenReturnResponse() {
        String prompt = "";
        when(chatModel.call(anyString())).thenReturn("Sorry, I can only talk about food.");

        String response = chatService.getResponse(prompt);

        assertThat(response).isInstanceOf(String.class);
        assertThat(response).isNotBlank();
    }
}
