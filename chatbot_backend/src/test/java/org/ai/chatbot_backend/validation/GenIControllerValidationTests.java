package org.ai.chatbot_backend.validation;

import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.controller.GenAIController;
import org.ai.chatbot_backend.service.ChatService;
import org.ai.chatbot_backend.service.ImageService;
import org.ai.chatbot_backend.service.RecipeService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Objects;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@WebMvcTest(GenAIController.class)
public class GenIControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;
    @MockitoBean
    private ImageService imageService;
    @MockitoBean
    private RecipeService recipeService;

    @ParameterizedTest
    @CsvSource({
            "vivid",
            "natural"
    })
    void whenValidStyles_thenReturnsImageUrl(String style) throws Exception {
        ImageResponse mockImageResponse = mock(ImageResponse.class, RETURNS_DEEP_STUBS);
        when(mockImageResponse.getResult().getOutput().getUrl()).thenReturn("https://dalleprodsec.blob.core.");

        when(imageService.generateDishImageFromParams(eq("Pizza"), isNull(), isNull(), isNull(), eq(style),
                eq(1024), eq(1024)))
                .thenReturn(mockImageResponse);

        mockMvc.perform(post("/api/v1/food-images")
                .param("name", "Pizza")
                .param("style", style)
                .param("height", "1024")
                .param("width", "1024"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.startsWith("https://dalleprodsec.blob.core.")));
    }

    @Test
    void testInvalidStyleReturnsError() throws Exception {
        mockMvc.perform(post("/api/v1/food-images")
                .param("name", "Pizza")
                .param("style", "cartoon")
                .param("height", "1024")
                .param("width", "1024"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(Objects.requireNonNull(result.getResolvedException()).
                        getMessage()).contains("Sorry, the picked style is invalid"));
    }

    @ParameterizedTest
    @CsvSource({
            "1024,1024",
            "1024,1792",
            "1792,1024"
    })
    void whenValidSizes_thenReturnsImageUrl(int width, int height) throws Exception {
        ImageResponse mockImageResponse = mock(ImageResponse.class, RETURNS_DEEP_STUBS);
        when(mockImageResponse.getResult().getOutput().getUrl()).thenReturn("https://dalleprodsec.blob.core.");

        when(imageService.generateDishImageFromParams(eq("Pizza"), isNull(), isNull(), isNull(), eq("vivid"),
                eq(height), eq(width)))
                .thenReturn(mockImageResponse);

        mockMvc.perform(post("/api/v1/food-images")
                        .param("name", "Pizza")
                        .param("style", "vivid")
                        .param("width", String.valueOf(width))
                        .param("height", String.valueOf(height)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.startsWith("https://dalleprodsec.blob.core.")));
    }

    @Test
    void whenEmptyImageResponse_thenReturnsErrorMessage() throws Exception {
        ImageResponse mockImageResponse = mock(ImageResponse.class, RETURNS_DEEP_STUBS);
        when(mockImageResponse.getResults()).thenReturn(new ArrayList<>());

        when(imageService.generateDishImageFromParams(eq("Pizza"), isNull(), isNull(), isNull(), eq("vivid"),
                eq(1024), eq(1024)))
                .thenReturn(mockImageResponse);

        mockMvc.perform(post("/api/v1/food-images")
                .param("name", "Pizza")
                .param("style", "vivid")
                .param("width", "1024")
                .param("height", "1024"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(Objects.requireNonNull(result.getResolvedException()).
                        getMessage()).contains("Sorry, I can't help with that request."));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "Pizza,vivid,1024,1024,main,null,null",
            "Pizza,vivid,1024,1024,null,cheese,null",
            "Pizza,vivid,1024,1024,null,null,flatbread",
            "Pizza,vivid,1024,1024,main,cheese,flatbread",
            "Pizza,vivid,1024,1024,main,cheese,null"
    })
    void whenOptionalParamsPresent_thenReturnsImageUrl(String name, String style, int height, int width, String course,
                                                       String mainIngredient, String dishType) throws Exception {
        ImageResponse mockImageResponse = mock(ImageResponse.class, RETURNS_DEEP_STUBS);
        when(mockImageResponse.getResult().getOutput().getUrl()).thenReturn("https://dalleprodsec.blob.core.");
        when(imageService.generateDishImageFromParams(eq(name), eq(course), eq(mainIngredient), eq(dishType), eq(style),
                eq(height), eq(width)))
                .thenReturn(mockImageResponse);
        mockMvc.perform(post("/api/v1/food-images")
                .param("name", name)
                .param("style", style)
                .param("height", String.valueOf(height))
                .param("width", String.valueOf(width))
                .param("course", course)
                .param("mainIngredient", mainIngredient)
                .param("dishType", dishType))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.startsWith("https://dalleprodsec.blob.core.")));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "illegal substances,vivid,1024,1024,null,null,null",
            "Pizza,vivid,1024,1024,illegal substances,null,null",
            "Pizza,vivid,1024,1024,null,illegal substances,null",
            "Pizza,vivid,1024,1024,null,null,illegal substances",
            "illegal substances,vivid,1024,1024,illegal substances,illegal substances,illegal substances",
            "dkjhdkhd,vivid,1024,1024,null,null,null",
            "Pizza,vivid,1024,1024,dkjhdkhd,null,null",
            "Pizza,vivid,1024,1024,null,dkjhdkhd,null",
            "Pizza,vivid,1024,1024,null,null,dkjhdkhd",
            "dkjhdkhd,vivid,1024,1024,dkjhdkhd,dkjhdkhd,dkjhdkhd"
    })
    void whenForbiddenOrNonsenseParams_thenReturnsError(String name, String style, int height, int width, String course,
                                                        String mainIngredient, String dishType) throws Exception {
        ImageResponse mockImageResponse = mock(ImageResponse.class, RETURNS_DEEP_STUBS);
        when(mockImageResponse.getResults()).thenReturn(new ArrayList<>());
        when(imageService.generateDishImageFromParams(eq(name), eq(course), eq(mainIngredient), eq(dishType),
                eq(style), eq(height), eq(width)))
                .thenReturn(mockImageResponse);
        mockMvc.perform(post("/api/v1/food-images")
                .param("name", name)
                .param("style", style)
                .param("height", String.valueOf(height))
                .param("width", String.valueOf(width))
                .param("course", course)
                .param("mainIngredient", mainIngredient)
                .param("dishType", dishType))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(Objects.requireNonNull(result.getResolvedException())
                        .getMessage()).contains("Sorry, I can't help with that request."));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "null,vivid,1024,1024,main,cheese,flatbread",
            "Pizza,null,1024,1024,main,cheese,flatbread",
            "Pizza,vivid,null,1024,main,cheese,flatbread",
            "Pizza,vivid,1024,null,main,cheese,flatbread"
    })
    void whenMissingRequiredParams_thenReturnsBadRequest(String name, String style, String height, String width,
                                                         String course, String mainIngredient, String dishType) throws Exception {

        log.info("name = {}, style = {}, height = {}, width = {}, course = {}, main = {}, dishType = {}", name, style,
                height, width, course, mainIngredient, dishType);

        when(imageService.generateDishImageFromParams(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new ImageResponse(new ArrayList<>()));

        mockMvc.perform(post("/api/v1/food-images")
                .param("name", name)
                .param("style", style)
                .param("height", height)
                .param("width", width)
                .param("course", course)
                .param("mainIngredient", mainIngredient)
                .param("dishType", dishType))
                .andExpect(status().isBadRequest());
    }
}
