package org.ai.chatbot_backend.integration;

import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.RecipeService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
public class RecipeServiceIntegrationTests {

    @Autowired
    private RecipeService recipeService;

    @ParameterizedTest
    @CsvSource(value = {
            "chicken, potato, carrots, cheese;French;tomato allergy",
            "chicken, potato, carrots, cheese;French;null",
            "chicken, potato, carrots, cheese;null;tomato allergy",
            "chicken, potato, carrots, cheese;null;null",
            "chicken, potato, carrots, cheese;null;dkjhdkhd",
            "chicken, potato, carrots, cheese;dkjhdkhd;null",
            "chicken, potato, carrots, cheese;dkjhdkhd;dkjhdkhd"
    }, delimiter = ';')
    public void whenGivenValidParams_thenReturnRecipe(String ingredients, String cuisine, String dietaryRestrictions) {

        String recipe = recipeService.createRecipe(ingredients, cuisine, dietaryRestrictions);
        log.info(recipe);

        assertThat(recipe).contains("Ingredients");
        assertThat(recipe).contains("Instructions");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "dkjhdkhd;null;null",
            "dkjhdkhd;dkjhdkhd;null",
            "dkjhdkhd;null;dkjhdkhd",
            "dkjhdkhd;dkjhdkhd;dkjhdkhd",
            "dkjhdkhd;French;null",
            "dkjhdkhd;null;tomato allergy",
            "dkjhdkhd;French;tomato allergy",
    }, delimiter = ';')
    public void whenGivenInvalidParams_thenPointItOut(String ingredients, String cuisine, String dietaryRestrictions) {

        assertThatThrownBy(() ->recipeService.createRecipe(ingredients, cuisine, dietaryRestrictions))
                .isInstanceOf(InappropriateRequestRefusalException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "illegal substances;null;null",
            "illegal substances;dkjhdkhd;null",
            "illegal substances;null;dkjhdkhd",
            "illegal substances;dkjhdkhd;dkjhdkhd",
            "illegal substances;French;null",
            "illegal substances;null;tomato allergy",
            "illegal substances;French;tomato allergy"
    }, delimiter = ';')
    public void whenGivenForbiddenParams_thenRefuseToGenerateRecipe(String ingredients, String cuisine, String dietaryRestrictions) {
        assertThatThrownBy(() ->recipeService.createRecipe(ingredients, cuisine, dietaryRestrictions))
                .isInstanceOf(InappropriateRequestRefusalException.class);
    }
}
