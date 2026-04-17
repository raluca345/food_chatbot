package org.ai.chatbot_backend.integration;

import com.openai.errors.OpenAIException;
import org.ai.chatbot_backend.dto.FoodImageRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class ImageServiceIntegrationTests {

    @Autowired
    ImageService imageService;

    @MockitoBean
    private ImageModel imageModel;

    @ParameterizedTest
    @CsvSource(value = {
            "braised pork;null;null;null;vivid;1024x1024",
            "chicken soup;soup;chicken;broth;natural;1024x1024",
            "vegetable stir fry;null;vegetables;null;vivid;1024x1024"
    }, delimiter = ';', nullValues = "null")
    public void whenGivenValidParams_thenReturnImage(String name, String course, String ingredients, String dishType,
                                                        String style, String size) {
        ImageResponse response = mock(ImageResponse.class);
        ImageGeneration generation = mock(ImageGeneration.class);
        Image output = mock(Image.class);
        when(response.getResults()).thenReturn(java.util.List.of(generation));
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(output);
        when(output.getUrl()).thenReturn("https://dalleprodsec.blob.core.windows.net/mock.png");
        when(imageModel.call(any())).thenReturn(response);

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
        assertThat(imageUrl).startsWith("https://dalleprodsec.blob.core.");
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
        when(imageModel.call(any())).thenThrow(new OpenAIException("Provider refused"));
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
