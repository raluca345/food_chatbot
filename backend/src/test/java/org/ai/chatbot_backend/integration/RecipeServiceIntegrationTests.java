package org.ai.chatbot_backend.integration;

import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.RecipeRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.RecipeService;
import org.ai.chatbot_backend.util.TestJsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class RecipeServiceIntegrationTests {

    @Autowired
    private RecipeService recipeService;

    @MockitoBean
    private ChatModel chatModel;

    private RecipeRequest recipeRequest(String ingredients, String cuisine, String dietaryRestrictions) {
        RecipeRequest request = new RecipeRequest();
        request.setIngredients(ingredients);
        request.setCuisine(cuisine);
        request.setDietaryRestrictions(dietaryRestrictions);
        return request;
    }

    private void mockModelJsonResponse(Map<String, Object> payload) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        AssistantMessage message = mock(AssistantMessage.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(TestJsonUtils.toJson(payload));
    }

    @Test
    public void whenModelReturnsValidRecipeJson_thenReturnRecipe() {
        String recipeMarkdown = """
                ### Lemon Garlic Chicken

                #### Ingredients:
                - chicken
                - lemon

                #### Instructions:
                1. Season the chicken.
                2. Bake until cooked through.
                """;

        mockModelJsonResponse(Map.of(
                "title", "Lemon Garlic Chicken",
                "recipe_markdown", recipeMarkdown
        ));

        CreateRecipeResult result = recipeService.createRecipe(
                recipeRequest("chicken, lemon", "French", "null"), null
        );

        assertThat(result.getRecipeMarkdown()).contains("#### Ingredients:");
        assertThat(result.getRecipeMarkdown()).contains("#### Instructions:");
    }

    @Test
    public void whenModelReturnsUnknownField_thenIgnoreItAndParse() {
        String recipeMarkdown = """
                ### Tomato Pasta

                #### Ingredients:
                - pasta
                - tomatoes

                #### Instructions:
                1. Boil pasta.
                2. Add sauce.
                """;

        mockModelJsonResponse(Map.of(
                "title", "Tomato Pasta",
                "recipe_markdown", recipeMarkdown,
                "notes", "extra field should be ignored"
        ));

        CreateRecipeResult result = recipeService.createRecipe(
                recipeRequest("pasta, tomatoes", "Italian", "null"), null
        );

        assertThat(result.getRecipeMarkdown()).contains("Tomato Pasta");
    }

    @Test
    public void whenModelReturnsClarificationInsteadOfRecipe_thenThrowInappropriateRequestRefusal() {
        mockModelJsonResponse(Map.of(
                "title", "Need Clarification",
                "recipe_markdown", "Could you clarify the ingredients you want to use?"
        ));

        assertThatThrownBy(() -> recipeService.createRecipe(
                recipeRequest("dkjhdkhd", "null", "null"), null))
                .isInstanceOf(InappropriateRequestRefusalException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null;French;none",
            "'   ';French;none"
    }, delimiter = ';', nullValues = "null")
    public void whenIngredientsMissing_thenThrowInappropriateRequestRefusal(String ingredients, String cuisine, String dietaryRestrictions) {
        assertThatThrownBy(() -> recipeService.createRecipe(
                recipeRequest(ingredients, cuisine, dietaryRestrictions), null))
                .isInstanceOf(InappropriateRequestRefusalException.class);
    }

    @Test
    public void whenModelReturnsForbiddenRefusal_thenThrowInappropriateRequestRefusal() {
        mockModelJsonResponse(Map.of(
                "title", "Refusal",
                "recipe_markdown", "I'm sorry, but I can't assist with that request."
        ));

        assertThatThrownBy(() -> recipeService.createRecipe(
                recipeRequest("illegal substances", "French", "null"), null))
                .isInstanceOf(InappropriateRequestRefusalException.class);
    }
}
