package org.ai.chatbot_backend.integration;

import org.ai.chatbot_backend.dto.FoodImageRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class ImageServiceIntegrationTests {

    @Autowired
    ImageService imageService;

    @MockitoBean
    private RestClient restClient;

    @ParameterizedTest
    @CsvSource(value = {
            "braised pork;null;null;null;vivid;1024x1024",
            "chicken soup;soup;chicken;broth;natural;1024x1024",
            "vegetable stir fry;null;vegetables;null;vivid;1024x1024"
    }, delimiter = ';', nullValues = "null")
    public void whenGivenValidParams_thenReturnImage(String name, String course, String ingredients, String dishType,
                                                        String style, String size) {
        Map<String, Object> mockResponse = Map.of(
                "data", List.of(
                        Map.of("url", "https://example.com/generated-image.png")
                )
        );

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(mockResponse);

        FoodImageRequest request = new FoodImageRequest();
        request.setName(name);
        request.setCourse(course);
        request.setIngredients(ingredients);
        request.setDishType(dishType);
        request.setStyle(style);
        request.setSize(size);

        String imageUrl = imageService.generateFoodImageFromParams(request);
        assertThat(imageUrl).isNotNull();
        assertThat(imageUrl).isInstanceOf(String.class);
        assertThat(imageUrl).startsWith("https://");
    }

    @ParameterizedTest
    @CsvSource(value ={
            "illegal substances,null,null,null,vivid,1024x1024",
            "illegal substances,dkjhdkhd,null,null,vivid,1024x1024",
            "illegal substances,null,dkjhdkhd,null,vivid,1024x1024",
            "illegal substances,null,null,dkjhdkhd,vivid,1024x1024",
            "illegal substances,dkjhdkhd,dkjhdkhd,dkjhdkhd,vivid,1024x1024",
            "dkjhdkhd,null,null,null,vivid,1024x1024",
            "dkjhdkhd,dkjhdkhd,null,null,vivid,1024x1024",
            "dkjhdkhd,null,dkjhdkhd,null,vivid,1024x1024",
            "dkjhdkhd,null,null,dkjhdkhd,vivid,1024x1024",
            "dkjhdkhd,dkjhdkhd,dkjhdkhd,dkjhdkhd,vivid,1024x1024"
    }, nullValues = "null")
    public void whenGivenForbiddenParams_thenRefuseToGenerateImage(String name, String course, String ingredients,
                                                                   String dishType, String style, String size) {
        Map<String, Object> emptyResponse = Map.of("data", List.of());

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(emptyResponse);

        FoodImageRequest request = new FoodImageRequest();
        request.setName(name);
        request.setCourse(course);
        request.setIngredients(ingredients);
        request.setDishType(dishType);
        request.setStyle(style);
        request.setSize(size);

        assertThatThrownBy(() ->
            imageService.generateFoodImageFromParams(request)
        ).isInstanceOf(InappropriateRequestRefusalException.class);
    }
}


