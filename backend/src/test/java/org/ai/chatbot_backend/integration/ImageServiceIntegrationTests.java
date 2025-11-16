package org.ai.chatbot_backend.integration;

import com.azure.core.exception.HttpResponseException;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.service.implementations.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.azure.openai.AzureOpenAiImageModel;

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
    private AzureOpenAiImageModel azureOpenAiImageModel;

    @ParameterizedTest
    @CsvSource(value = {
            "braised pork;null;null;null;vivid;1024x1024",
            "chicken soup;soup;chicken;broth;natural;1024x1024",
            "vegetable stir fry;null;vegetables;null;vivid;1024x1024"
    }, delimiter = ';', nullValues = "null")
    public void whenGivenValidParams_thenReturnImage(String name, String course, String ingredients, String dishType,
                                                        String style, String size) {
        String imageUrl = imageService.generateFoodImageFromParams(name, course, ingredients, dishType, style,
                                                                size);
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
        HttpResponseException httpEx = mock(HttpResponseException.class);
        when(azureOpenAiImageModel.call(any())).thenThrow(httpEx);
        assertThatThrownBy(() ->
            imageService.generateFoodImageFromParams(name, course, ingredients, dishType, style, size)
        ).isInstanceOf(InappropriateRequestRefusalException.class);
    }
}
