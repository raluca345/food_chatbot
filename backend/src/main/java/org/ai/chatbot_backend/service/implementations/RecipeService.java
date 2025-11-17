package org.ai.chatbot_backend.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.CreateRecipeResult;
import org.ai.chatbot_backend.dto.RecipeResponse;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.service.interfaces.IRecipeService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RecipeService implements IRecipeService {
    private final ChatModel chatModel;
    private final RecipeFileService recipeFileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    private static final Pattern RECIPE_PATTERN = Pattern.compile(
            "(?s)^###\\s+.+?\\R+####\\s+Ingredients:.*?####\\s+Instructions:",
            Pattern.CASE_INSENSITIVE
    );

    private static Prompt getSystemPrompt(String ingredients, String cuisine, String dietaryRestrictions) {
        var template = """
            You are a helpful and professional chef assistant.
            Create a recipe using the following information:

            **Ingredients:** {ingredients}
            **Cuisine:** {cuisine}
            **Dietary restrictions:** {dietaryRestrictions}

            Respond **only in valid JSON** with these fields:
            - "title": the recipe title
            - "recipe_markdown": the full recipe formatted exactly like this example:
              ### Lemon Garlic Butter Baked Fish

              #### Ingredients:
              - 4 fish fillets (such as cod, tilapia, or haddock)
              - 3 tablespoons unsalted butter, melted
              - 3 cloves garlic, minced
              - 1 lemon (zested and juiced)
              - 1 teaspoon dried parsley
              - Salt and pepper to taste

              #### Instructions:
              1. Preheat oven to 400°F (200°C)...
              2. Prepare sauce...
              3. Bake...

            Make sure the JSON is valid — no extra commentary, greetings, or text outside the JSON.
            If any ingredients are nonsensical, point it out politely and ask the user for clarification.
            """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Map<String, Object> params = Map.of(
                "ingredients", ingredients,
                "cuisine", cuisine,
                "dietaryRestrictions", dietaryRestrictions
        );
        return promptTemplate.create(params);
    }

    private String extractJsonBlock(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private boolean isValidRecipeFormat(String recipeText) {
        if (recipeText == null || recipeText.isBlank()) {
            return false;
        }

        if (recipeText.toLowerCase().contains("i cannot") ||
                recipeText.toLowerCase().contains("i'm sorry") ||
                recipeText.toLowerCase().contains("unable to") ||
                recipeText.toLowerCase().contains("clarify") ||
                recipeText.toLowerCase().contains("illegal") ||
                recipeText.toLowerCase().contains("inappropriate")) {
            return false;
        }

        if (RECIPE_PATTERN.matcher(recipeText).find()) {
            return true;
        }

        String lower = recipeText.toLowerCase();
        boolean hasTitle = recipeText.trim().startsWith("###");
        boolean hasIngredients = lower.contains("ingredients");
        boolean hasInstructions = lower.contains("instructions");

        return hasTitle && hasIngredients && hasInstructions;
    }

    @Override
    public CreateRecipeResult createRecipe(String ingredients, String cuisine, String dietaryRestrictions) {
        Prompt prompt = getSystemPrompt(ingredients, cuisine, dietaryRestrictions);

        try {
            String rawResponse = chatModel.call(prompt).getResult().getOutput().getText();
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new ResourceNotFoundException("No recipe");
            }
            String jsonResponse = extractJsonBlock(rawResponse);

            RecipeResponse recipeResponse = objectMapper.readValue(jsonResponse, RecipeResponse.class);

            if (!isValidRecipeFormat(recipeResponse.getRecipeMarkdown())) {
                throw new InappropriateRequestRefusalException(
                        "I'm sorry, but I can't assist with that request."
                );
            }

            Long id = recipeFileService.storeRecipeText(recipeResponse.getRecipeMarkdown());
            String downloadUrl = recipeFileService.getDownloadMarkdown(id, backendBaseUrl);

            return new CreateRecipeResult(recipeResponse.getRecipeMarkdown(), id, downloadUrl);

        } catch (InappropriateRequestRefusalException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InappropriateRequestRefusalException("I'm sorry, but I can't assist with that request.");
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse recipe JSON: " + e.getMessage(), e);
        }
    }


    public String extractRecipeTitle(String markdown) {
        Pattern pattern = Pattern.compile("^###\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Untitled Recipe";
    }

}
