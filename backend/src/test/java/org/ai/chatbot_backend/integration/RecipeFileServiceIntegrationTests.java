package org.ai.chatbot_backend.integration;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.ChatService;
import org.ai.chatbot_backend.service.implementations.RecipeFileService;
import org.ai.chatbot_backend.service.implementations.RecipeService;
import org.junit.jupiter.api.Nested;
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
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RecipeFileServiceIntegrationTests {

    @Autowired
    private ChatService chatService;

    @Nested
    class RecipeServiceTests {

        @MockitoBean
        private ChatModel chatModel;

        @MockitoSpyBean
        private RecipeFileService recipeFileService;

        @Autowired
        private RecipeService recipeService;

        @ParameterizedTest
        @CsvSource(value = {
                """
                        Classic Omelette
                        
                        Ingredients:
                        - 2 eggs
                        - salt
                        - pepper
                        
                        Instructions:
                        1. Beat eggs.
                        2. Cook in pan.
                        """
        })
        void whenValidRecipe_thenDownloadUrlAndFileAreValid(String recipe) throws Exception {
            ChatResponse response = mock(ChatResponse.class);
            Generation generation = mock(Generation.class);
            AssistantMessage message = new AssistantMessage(recipe);

            when(chatModel.call(any(Prompt.class))).thenReturn(response);
            when(response.getResult()).thenReturn(generation);
            when(generation.getOutput()).thenReturn(message);

            String result = recipeService.createRecipe("egss", "French", "null");
            assertThat(result).contains(recipe);

            Matcher m = Pattern.compile("\\[Download recipe]\\(([^)]+)\\)").matcher(result);
            assertThat(m.find()).as("Download link is present").isTrue();
            String downloadUrl = m.group(1);
            assertThat(downloadUrl).startsWith("http://localhost:8080/api/v1/recipes/download/");

            long id = Long.parseLong(downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));

            Resource resource = recipeFileService.getRecipeFile(id);
            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();

            String fileText = new String(resource.getInputStream().readAllBytes());
            assertThat(fileText).isEqualTo(recipe);
        }

        @Test
        void whenIllegalOrRefusedInput_thenThrowsAndNoFileOrUrlGenerated() {
            HttpResponse mockResponse = mock(HttpResponse.class);
            when(chatModel.call(any(Prompt.class))).thenThrow(new HttpResponseException("refused", mockResponse));

            assertThatThrownBy(() -> recipeService.createRecipe("illegal substances", "null", "null")).isInstanceOf(InappropriateRequestRefusalException.class)
                    .hasMessageContaining("I'm sorry, but I can't assist with that request.");

            verifyNoInteractions(recipeFileService);
        }
    }

    // java
    @Nested
    class ChatModelTests {
        @MockitoBean
        private ChatModel chatModel;

        @MockitoSpyBean
        private RecipeFileService recipeFileService;

        @ParameterizedTest
        @CsvSource(value = {
                """
                        Classic Omelette
                        
                        Ingredients:
                        - 2 eggs
                        - salt
                        - pepper
                        
                        Instructions:
                        1. Beat eggs.
                        2. Cook in pan.
                        """
        })
        void whenValidRecipe_thenDownloadUrlAndFileAreValid(String recipe) throws Exception {

            Long id = recipeFileService.storeRecipeText(recipe);
            String downloadMd = recipeFileService.getDownloadMarkdown(id, "http://localhost:8080");
            String modelOut = recipe + "\n\nYou can download this recipe here: " + downloadMd;

            when(chatModel.call(any(String.class))).thenReturn(modelOut);

            String result = chatModel.call("Give me an omelette recipe.");
            assertThat(result).contains(recipe);

            Matcher m = Pattern.compile("\\[Download recipe]\\(([^)]+)\\)").matcher(result);
            assertThat(m.find()).as("Download link is present").isTrue();
            String downloadUrl = m.group(1);
            assertThat(downloadUrl).startsWith("http://localhost:8080/api/v1/recipes/download/");

            long parsedId = Long.parseLong(downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));

            Resource resource = recipeFileService.getRecipeFile(parsedId);
            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();

            String fileText = new String(resource.getInputStream().readAllBytes());
            assertThat(fileText).isEqualTo(recipe);
        }


        @Test
        void whenIllegalOrRefusedInput_thenThrowsAndNoFileOrUrlGenerated() {
            HttpResponse mockResponse = mock(HttpResponse.class);
            when(chatModel.call(any(Prompt.class))).thenThrow(new HttpResponseException("refused", mockResponse));

            assertThat(chatService.getResponse("Give me a recipe containing illegal substances."))
                    .isEqualTo("Sorry, I can only answer questions about food.");

            verifyNoInteractions(recipeFileService);
        }
    }
}
