package org.ai.chatbot_backend.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from login endpoint. Use: Bearer {token}")));
    }

    @Bean
    public OperationCustomizer keepOnlyDocumentedResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
                return operation;
            }

            Set<String> documentedResponseCodes = collectDocumentedResponseCodes(handlerMethod);
            if (documentedResponseCodes.isEmpty()) {
                return operation;
            }

            operation.getResponses().entrySet()
                    .removeIf(entry -> !documentedResponseCodes.contains(entry.getKey()));
            return operation;
        };
    }

    @Bean
    public OpenApiCustomizer keepOnlyDocumentedResponsesOpenApiCustomizer(
            RequestMappingHandlerMapping handlerMapping) {
        return openApi -> {
            if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
                return;
            }

            Map<String, Set<String>> documentedCodesByRoute = buildDocumentedCodesByRoute(handlerMapping);
            openApi.getPaths().forEach((openApiPath, pathItem) -> {
                if (pathItem == null) {
                    return;
                }

                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if (operation == null || operation.getResponses() == null || operation.getResponses().isEmpty()) {
                        return;
                    }

                    String key = routeKey(httpMethod.name(), openApiPath);
                    Set<String> documentedCodes = documentedCodesByRoute.get(key);
                    if (documentedCodes == null || documentedCodes.isEmpty()) {
                        return;
                    }

                    operation.getResponses().entrySet()
                            .removeIf(entry -> !documentedCodes.contains(entry.getKey()));
                });
            });
        };
    }

    private Map<String, Set<String>> buildDocumentedCodesByRoute(RequestMappingHandlerMapping handlerMapping) {
        Map<String, Set<String>> documentedCodesByRoute = new HashMap<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            Set<String> documentedCodes = collectDocumentedResponseCodes(handlerMethod);

            if (documentedCodes.isEmpty()) {
                continue;
            }

            Set<String> paths = mappingInfo.getPatternValues();
            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();

            for (String path : paths) {
                if (methods.isEmpty()) {
                    continue;
                }

                for (RequestMethod method : methods) {
                    addRouteResponses(documentedCodesByRoute, routeKey(method.name(), path), documentedCodes);
                }
            }
        }
        return documentedCodesByRoute;
    }

    private void addRouteResponses(
            Map<String, Set<String>> documentedCodesByRoute, String routeKey, Set<String> documentedCodes) {
        documentedCodesByRoute
                .computeIfAbsent(routeKey, key -> new LinkedHashSet<>())
                .addAll(documentedCodes);
    }

    private String routeKey(String httpMethod, String path) {
        return httpMethod.toUpperCase() + " " + normalizePath(path);
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }

        String normalized = path.trim();
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private Set<String> collectDocumentedResponseCodes(HandlerMethod handlerMethod) {
        Set<String> responseCodes = new LinkedHashSet<>();
        addResponseCodes(handlerMethod.getBeanType(), responseCodes);
        addResponseCodes(handlerMethod.getMethod(), responseCodes);
        return responseCodes;
    }

    private void addResponseCodes(AnnotatedElement element, Set<String> responseCodes) {
        ApiResponses apiResponses = AnnotatedElementUtils.findMergedAnnotation(element, ApiResponses.class);
        if (apiResponses != null) {
            for (ApiResponse apiResponse : apiResponses.value()) {
                addResponseCode(apiResponse, responseCodes);
            }
        }

        for (ApiResponse apiResponse : AnnotatedElementUtils.findAllMergedAnnotations(element, ApiResponse.class)) {
            addResponseCode(apiResponse, responseCodes);
        }

        Operation operation = AnnotatedElementUtils.findMergedAnnotation(element, Operation.class);
        if (operation != null) {
            for (ApiResponse apiResponse : operation.responses()) {
                addResponseCode(apiResponse, responseCodes);
            }
        }
    }

    private void addResponseCode(ApiResponse apiResponse, Set<String> responseCodes) {
        if (apiResponse == null) {
            return;
        }

        if (StringUtils.hasText(apiResponse.responseCode())) {
            responseCodes.add(apiResponse.responseCode().trim());
        }
    }
}
