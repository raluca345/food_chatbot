package org.ai.chatbot_backend.integration;

import org.ai.chatbot_backend.service.ImageService;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@SpringBootTest
public class ImageServiceIntegrationTests {

    @Autowired
    ImageService imageService;


    @ParameterizedTest
    @CsvSource({
            "braised pork,null,null,null,vivid,1024,1024",
            "chicken soup,soup,chicken,broth,natural,1024,1024",
            "vegetable stir fry,null,vegetables,null,vivid,1024,1024"
    })
    public void whenGivenValidParams_thenReturnImage(String name, String course, String mainIngredient, String dishType,
                                                        String style, int height, int width) {
        ImageResponse response = imageService.generateDishImageFromParams(name, course, mainIngredient, dishType, style,
                                                                height, width);
        assertThat(response).isNotNull();
        assertThat(response.getResult().getOutput().getUrl()).isInstanceOf(String.class);
        assertThat(response.getResult().getOutput().getUrl()).startsWith("https://dalleprodsec.blob.core.");
    }

    @ParameterizedTest
    @CsvSource({
            "illegal substances,null,null,null,vivid,1024,1024",
            "illegal substances,dkjhdkhd,null,null,vivid,1024,1024",
            "illegal substances,null,dkjhdkhd,null,vivid,1024,1024",
            "illegal substances,null,null,dkjhdkhd,vivid,1024,1024",
            "illegal substances,dkjhdkhd,dkjhdkhd,dkjhdkhd,vivid,1024,1024",
            "dkjhdkhd,null,null,null,vivid,1024,1024",
            "dkjhdkhd,dkjhdkhd,null,null,vivid,1024,1024",
            "dkjhdkhd,null,dkjhdkhd,null,vivid,1024,1024",
            "dkjhdkhd,null,null,dkjhdkhd,vivid,1024,1024",
            "dkjhdkhd,dkjhdkhd,dkjhdkhd,dkjhdkhd,vivid,1024,1024"
    })
    public void whenGivenForbiddenParams_thenRefuseToGenerateImage(String name, String course, String mainIngredient, String dishType,
                                                        String style, int height, int width) {
        ImageResponse response = imageService.generateDishImageFromParams(name, course, mainIngredient, dishType, style,
                                                                height, width);
        assertThat(response.getResults()).isEmpty();
    }
}
