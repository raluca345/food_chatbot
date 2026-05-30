package org.ai.chatbot_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Customizer that adds detailed examples and schemas to OpenAPI documentation
 * based on operation IDs and method paths.
 */

@Configuration
public class SwaggerExamplesCustomizer implements OpenApiCustomizer {

    private static final Map<String, EndpointDocumentation> ENDPOINT_DOCS = new HashMap<>();

    static {
        // AI & Conversations endpoints
        ENDPOINT_DOCS.put("POST /api/v1/chat", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getStartConversationExample())
                .responseExample(SwaggerSchemaConfig.getRecipeAuthenticatedExample()));

        ENDPOINT_DOCS.put("POST /api/v1/chat/guest", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getGuestChatExample())
                .responseExample(SwaggerSchemaConfig.getRecipeGuestExample()));

        ENDPOINT_DOCS.put("POST /api/v1/chat/{conversationId}/messages", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getStartConversationExample()));

        ENDPOINT_DOCS.put("PATCH /api/v1/chat/{conversationId}", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getUpdateTitleExample()));

        ENDPOINT_DOCS.put("POST /api/v1/recipes", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getRecipeGenerationExample()));

        ENDPOINT_DOCS.put("POST /api/v1/recipes/download/guest", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getRecipeDownloadExample()));

        ENDPOINT_DOCS.put("POST /api/v1/food-images", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getFoodImageExample()));

        // Authentication endpoints
        ENDPOINT_DOCS.put("POST /api/v1/auth/register", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getRegisterExample()));

        ENDPOINT_DOCS.put("POST /api/v1/auth/login", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getLoginExample()));

        // Password Reset endpoints
        ENDPOINT_DOCS.put("POST /api/v1/auth/password-reset/request", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getPasswordResetRequestExample()));

        ENDPOINT_DOCS.put("POST /api/v1/auth/password-reset/confirm", new EndpointDocumentation()
                .requestExample(SwaggerSchemaConfig.getPasswordResetConfirmExample()));
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
            return;
        }

        openApi.getPaths().forEach((path, pathItem) -> {
            if (pathItem == null) {
                return;
            }

            pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                if (operation == null) {
                    return;
                }

                String key = httpMethod.name() + " " + path;
                EndpointDocumentation doc = ENDPOINT_DOCS.get(key);
                if (doc != null) {
                    applyDocumentation(operation, doc);
                }
            });
        });
    }

    private void applyDocumentation(Operation operation, EndpointDocumentation doc) {
        // Apply request body example if present
        if (doc.requestExample != null && operation.getRequestBody() != null) {
            applyRequestExample(operation, doc.requestExample);
        }

        // Apply response examples if present
        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            if (doc.responseExample != null) {
                ApiResponse successResponse = operation.getResponses().get("200");
                if (successResponse != null) {
                    applyResponseExample(successResponse, doc.responseExample);
                }
                // Also try 201 for created responses
                successResponse = operation.getResponses().get("201");
                if (successResponse != null) {
                    applyResponseExample(successResponse, doc.responseExample);
                }
            }
        }
    }

    private void applyRequestExample(Operation operation, String example) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return;
        }

        operation.getRequestBody().getContent().forEach((mediaType, content) -> {
            if (content.getSchema() != null) {
                content.example(example);
            }
        });
    }

    private void applyResponseExample(ApiResponse response, String example) {
        if (response.getContent() == null) {
            return;
        }

        response.getContent().forEach((mediaType, content) -> {
            if (content.getSchema() != null) {
                content.example(example);
            }
        });
    }

    /**
     * Data class to hold endpoint documentation
     */
    private static class EndpointDocumentation {
        String requestExample;
        String responseExample;

        EndpointDocumentation requestExample(String example) {
            this.requestExample = example;
            return this;
        }

        EndpointDocumentation responseExample(String example) {
            this.responseExample = example;
            return this;
        }
    }
}

