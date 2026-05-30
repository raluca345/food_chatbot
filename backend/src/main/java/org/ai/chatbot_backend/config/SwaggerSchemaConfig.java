package org.ai.chatbot_backend.config;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized definitions for Swagger examples and response schemas.
 * Used by OpenApiCustomizer to add detailed documentation to endpoints.
 */
@Configuration
public class SwaggerSchemaConfig {

    // Request Examples
    public static String getStartConversationExample() {
        return """
                {
                  "message": "What can I cook with eggs and cheese?"
                }
                """;
    }

    public static String getGuestChatExample() {
        return """
                {
                  "message": "Suggest a quick pasta recipe."
                }
                """;
    }

    public static String getUpdateTitleExample() {
        return """
                {
                  "title": "Weeknight Pasta Ideas"
                }
                """;
    }

    public static String getRecipeGenerationExample() {
        return """
                {
                  "ingredients": "chicken breast, tomatoes, garlic, basil, olive oil",
                  "cuisine": "mediterranean",
                  "dietaryRestrictions": "high-protein, low-carb"
                }
                """;
    }

    public static String getRecipeDownloadExample() {
        return """
                {
                  "recipeMarkdown": "## Ingredients\\n- eggs\\n- cheese\\n\\n## Instructions\\n1. Mix\\n2. Bake"
                }
                """;
    }

    public static String getFoodImageExample() {
        return """
                {
                  "name": "Margherita Pizza",
                  "style": "natural",
                  "size": "1024x1024",
                  "course": "main",
                  "ingredients": "tomato, mozzarella, basil",
                  "dishType": "pizza"
                }
                """;
    }

    public static String getRegisterExample() {
        return """
                {
                  "username": "test345",
                  "email": "test@example.com",
                  "password": "supersecret123"
                }
                """;
    }

    public static String getLoginExample() {
        return """
                {
                  "email": "test@example.com",
                  "password": "supersecret123"
                }
                """;
    }

    public static String getPasswordResetRequestExample() {
        return """
                {
                  "recipient": "test@example.com",
                  "msgBody": "Reset your password by clicking the link below.",
                  "subject": "Reset Password"
                }
                """;
    }

    public static String getPasswordResetConfirmExample() {
        return """
                {
                  "token": "1d35ff4a-2b34-457d-aa06-0ecb964e74c1",
                  "password": "newpassword123"
                }
                """;
    }

    public static String getRecipeAuthenticatedExample() {
        return """
                {
                  "recipeMarkdown": "### Mediterranean Chicken Bowl\\n\\n#### Ingredients:\\n- 2 chicken breasts\\n- 2 tomatoes\\n- 2 cloves garlic\\n- fresh basil\\n- olive oil\\n\\n#### Instructions:\\n1. Season and sear the chicken.\\n2. Saute garlic and tomatoes in olive oil.\\n3. Slice chicken, top with sauce and basil.",
                  "fileId": 42,
                  "downloadMarkdown": "[Download recipe](http://localhost:8080/api/v1/recipes/download/42)"
                }
                """;
    }

    public static String getRecipeGuestExample() {
        return """
                {
                  "recipeMarkdown": "### Mediterranean Chicken Bowl\\n\\n#### Ingredients:\\n- 2 chicken breasts\\n- 2 tomatoes\\n- 2 cloves garlic\\n- fresh basil\\n- olive oil\\n\\n#### Instructions:\\n1. Season and sear the chicken.\\n2. Saute garlic and tomatoes in olive oil.\\n3. Slice chicken, top with sauce and basil.",
                  "fileId": null,
                  "downloadMarkdown": null
                }
                """;
    }

    // Response Content Builders

    public static Content createJsonContent(String example) {
        MediaType mediaType = new MediaType()
                .schema(new Schema<>().type("object"))
                .example(example);
        return new Content()
                .addMediaType("application/json", mediaType);
    }

    public static Content createPlainTextContent(String example) {
        MediaType mediaType = new MediaType()
                .schema(new Schema<>().type("string"))
                .example(example);
        return new Content()
                .addMediaType("text/plain", mediaType);
    }

    public static ApiResponse createJsonResponse(String description, String example) {
        return new ApiResponse()
                .description(description)
                .content(createJsonContent(example));
    }

    public static ApiResponse createPlainTextResponse(String description, String example) {
        return new ApiResponse()
                .description(description)
                .content(createPlainTextContent(example));
    }

    public static ApiResponse createEmptyResponse(String description) {
        return new ApiResponse().description(description);
    }
}

