package org.ai.chatbot_backend.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ai.chatbot_backend.config.JwtService;
import org.ai.chatbot_backend.controller.GenAIController;
import org.ai.chatbot_backend.dto.FoodImageRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.implementations.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GenAIController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GenIControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;
    @MockitoBean
    private ImageService imageService;
    @MockitoBean
    private RecipeService recipeService;
    @MockitoBean
    private RecipeFileService recipeFileService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private RecipeHistoryService recipeHistoryService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private AuthHelper authHelper;

    private final ObjectMapper mapper = new ObjectMapper();

    private String foodImageRequestJson(
            String name,
            String style,
            String size,
            String course,
            String ingredients,
            String dishType
    ) throws Exception {
        FoodImageRequest request = new FoodImageRequest();
        request.setName(name);
        request.setStyle(style);
        request.setSize(size);
        request.setCourse(course);
        request.setIngredients(ingredients);
        request.setDishType(dishType);
        return mapper.writeValueAsString(request);
    }

    @ParameterizedTest
    @CsvSource({
            "vivid",
            "natural"
    })
    void whenValidStyles_thenReturnsImageUrl(String style) throws Exception {
        when(imageService.generateFoodImageFromParams(any(FoodImageRequest.class)))
                .thenReturn("https://dalleprodsec.blob.core.");

        mockMvc.perform(post("/api/v1/food-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodImageRequestJson("Pizza", style, "1024x1024", null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", Matchers.startsWith("https://dalleprodsec.blob.core.")));
    }

    @Test
    void testInvalidStyleReturnsError() throws Exception {
        when(imageService.generateFoodImageFromParams(any(FoodImageRequest.class)))
                .thenThrow(new InappropriateRequestRefusalException("Sorry, the picked style is invalid"));

        mockMvc.perform(post("/api/v1/food-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodImageRequestJson("Pizza", "cartoon", "1024x1024", null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Sorry, the picked style is invalid")));
    }

    @ParameterizedTest
    @CsvSource({
            "1024x1024",
            "1024x1792",
            "1792x1024"
    })
    void whenValidSizes_thenReturnsImageUrl(String size) throws Exception {
        when(imageService.generateFoodImageFromParams(any(FoodImageRequest.class)))
                .thenReturn("https://dalleprodsec.blob.core.");

        mockMvc.perform(post("/api/v1/food-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodImageRequestJson("Pizza", "vivid", size, null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", Matchers.startsWith("https://dalleprodsec.blob.core.")));
    }

    @Test
    void whenEmptyImageResponse_thenReturnsErrorMessage() throws Exception {
        when(imageService.generateFoodImageFromParams(any(FoodImageRequest.class)))
                .thenThrow(new InappropriateRequestRefusalException("Sorry, I can't help with that request."));

        mockMvc.perform(post("/api/v1/food-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodImageRequestJson("Pizza", "vivid", "1024x1024", null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Sorry, I can't help with that request.")));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "Pizza,vivid,1024x1024,main,null,null",
            "Pizza,vivid,1024x1024,null,cheese,null",
            "Pizza,vivid,1024x1024,null,null,flatbread",
            "Pizza,vivid,1024x1024,main,cheese,flatbread",
            "Pizza,vivid,1024x1024,main,cheese,null"
    })
    void whenOptionalParamsPresent_thenReturnsImageUrl(String name, String style, String size, String course,
                                                       String ingredients, String dishType) throws Exception {
        when(imageService.generateFoodImageFromParams(any(FoodImageRequest.class)))
                .thenReturn("https://dalleprodsec.blob.core.");
        mockMvc.perform(post("/api/v1/food-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodImageRequestJson(name, style, size, course, ingredients, dishType)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", Matchers.startsWith("https://dalleprodsec.blob.core.")));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "illegal substances,vivid,1024x1024,null,null,null",
            "Pizza,vivid,1024x1024,illegal substances,null,null",
            "Pizza,vivid,1024x1024,null,illegal substances,null",
            "Pizza,vivid,1024x1024,null,null,illegal substances",
            "illegal substances,vivid,1024x1024,illegal substances,illegal substances,illegal substances",
            "dkjhdkhd,vivid,1024x1024,null,null,null",
            "Pizza,vivid,1024x1024,dkjhdkhd,null,null",
            "Pizza,vivid,1024x1024,null,dkjhdkhd,null",
            "Pizza,vivid,1024x1024,null,null,dkjhdkhd",
            "dkjhdkhd,vivid,1024x1024,dkjhdkhd,dkjhdkhd,dkjhdkhd"
    })
    void whenForbiddenOrNonsenseParams_thenReturnsError(String name, String style, String size, String course,
                                                        String ingredients, String dishType) throws Exception {
        when(imageService.generateFoodImageFromParams(any(FoodImageRequest.class)))
                .thenThrow(new InappropriateRequestRefusalException("Sorry, I can't help with that request."));

        mockMvc.perform(post("/api/v1/food-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodImageRequestJson(name, style, size, course, ingredients, dishType)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Sorry, I can't help with that request.")));

    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "null,vivid,1024x1024,main,cheese,flatbread",
            "Pizza,null,1024x1024,main,cheese,flatbread",
            "Pizza,vivid,null,main,cheese,flatbread",
            "Pizza,vivid,1024x1024,main,cheese,flatbread"
    })
    void whenMissingRequiredParams_thenReturnsBadRequest(String name, String style, String size,
                                                         String course, String ingredients, String dishType) throws Exception {
        when(imageService.generateFoodImageFromParams(any(FoodImageRequest.class)))
                .thenThrow(new InappropriateRequestRefusalException("Sorry, I can't help with that request."));

        mockMvc.perform(post("/api/v1/food-images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foodImageRequestJson(name, style, size, course, ingredients, dishType)))
                .andExpect(status().isBadRequest());
    }
}
