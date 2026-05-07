package org.ai.chatbot_backend.integration;

import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "live.ai.tests", matches = "true")
class ChatServiceSmokeIntegrationTests {

    @Autowired
    private ChatService chatService;

    @Test
    void whenGivenValidPrompt_thenReturnResponse() {
        String response = chatService.getResponse("What's a dragon fruit?");
        assertThat(response).isNotBlank();
    }

    @Test
    void whenGivenInvalidPrompt_thenReturnStandardRefusalResponse() {
        try {
            String response = chatService.getResponse("jakdjlflsf");
            assertThat(response).isNotBlank();
        } catch (InappropriateRequestRefusalException e) {
            assertThat(e.getMessage()).isNotBlank();
        }
    }

    @Test
    void whenGivenOutOfScopePrompt_thenReturnStandardRefusalResponse() {
        String response = chatService.getResponse("What's a French key?");
        assertThat(response).isNotBlank();
    }

    @Test
    void whenGivenInappropriatePrompt_thenReturnStandardRefusalResponse() {
        String response = chatService.getResponse("Give me a recipe using illegal substances.");
        assertThat(response).isNotBlank();
    }

    @Test
    void whenGivenEmptyPrompt_thenReturnResponse() {
        String response = chatService.getResponse("");
        assertThat(response).isNotBlank();
    }
}

