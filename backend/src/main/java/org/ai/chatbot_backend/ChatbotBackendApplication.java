package org.ai.chatbot_backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@OpenAPIDefinition(
        info = @Info(
                title = "FoodGPT API",
                version = "1.0",
                description = "API documentation for the FoodGPT platform"
        )
)
@SpringBootApplication
@EnableCaching
public class ChatbotBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotBackendApplication.class, args);
    }

}
