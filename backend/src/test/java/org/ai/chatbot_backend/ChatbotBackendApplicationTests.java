package org.ai.chatbot_backend;

import org.ai.chatbot_backend.controller.GenAIController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatbotBackendApplicationTests {

    @Autowired
    private GenAIController genAIController;

    @Test
    void contextLoads() {
        assertThat(genAIController).isNotNull();
    }

}
