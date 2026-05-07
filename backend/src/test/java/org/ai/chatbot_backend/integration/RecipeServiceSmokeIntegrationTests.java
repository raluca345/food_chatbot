package org.ai.chatbot_backend.integration;

import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.RecipeRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "live.ai.tests", matches = "true")
public class RecipeServiceSmokeIntegrationTests {

    @Autowired
    private RecipeService recipeService;

    private RecipeRequest recipeRequest(String ingredients, String cuisine, String dietaryRestrictions) {
        RecipeRequest request = new RecipeRequest();
        request.setIngredients(ingredients);
        request.setCuisine(cuisine);
        request.setDietaryRestrictions(dietaryRestrictions);
        return request;
    }

    @Test
    public void liveModel_whenGivenValidIngredients_thenReturnNonEmptyResponse() {
        CreateRecipeResult result = recipeService.createRecipe(
                recipeRequest("chicken, potato, carrots", "French", "null"), null
        );

        assertThat(result.getRecipeMarkdown()).isNotBlank();
    }

    @Test
    public void liveModel_whenGivenLikelyInvalidIngredients_thenClarifiesOrRefusesButNotCrash() {
        try {
            CreateRecipeResult result = recipeService.createRecipe(
                    recipeRequest("dkjhdkhd", "null", "null"), null
            );
            assertThat(result.getRecipeMarkdown()).isNotBlank();
        } catch (InappropriateRequestRefusalException e) {
            assertThat(e.getMessage()).isNotBlank();
        }
    }
}

