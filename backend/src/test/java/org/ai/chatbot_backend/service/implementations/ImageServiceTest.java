package org.ai.chatbot_backend.service.implementations;

import org.ai.chatbot_backend.dto.FoodImageRequest;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.Image;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.repository.ImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ImageServiceTest {

    private ImageRepository imageRepository;
    private S3Client r2Client;
    private RestClient restClient;
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        UserService userService = mock(UserService.class);
        r2Client = mock(S3Client.class);
        R2Service r2Service = mock(R2Service.class);
        restClient = mock(RestClient.class);
        imageRepository = mock(ImageRepository.class);

        imageService = new ImageService(userService, r2Client, r2Service, restClient, imageRepository);
        ReflectionTestUtils.setField(imageService, "maiEndpoint", "https://example.com/mai/v1/images/generations");
        ReflectionTestUtils.setField(imageService, "maiApiKey", "test-key");
        ReflectionTestUtils.setField(imageService, "nonFoodKeywords", Set.of("hammer", "screw", "nail"));
    }

    private User user(long id) {
        User u = new User();
        u.setId(id);
        u.setName("test");
        u.setEmail("a@b.c");
        u.setPassword("p");
        return u;
    }

    @Test
    void deleteByIdForUser_success_deletesStorageAndDb() {
        User u = user(1L);
        Image img = Image.builder().id(10L).user(u).filename("users/1/images/foo.png").createdAt(LocalDateTime.now()).build();

        when(imageRepository.findById(10L)).thenReturn(Optional.of(img));

        imageService.deleteByIdForUser(10L, u);

        verify(r2Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        verify(imageRepository, times(1)).deleteById(10L);
    }

    @Test
    void deleteByIdForUser_notFound_throwsResourceNotFound() {
        User u = user(1L);
        when(imageRepository.findById(11L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> imageService.deleteByIdForUser(11L, u));
        verify(r2Client, never()).deleteObject((DeleteObjectRequest) any());
        verify(imageRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteByIdForUser_forbidden_throwsAccessDenied() {
        User owner = user(2L);
        User requester = user(1L);
        Image img = Image.builder().id(12L).user(owner).filename("users/2/images/bar.png")
                .createdAt(LocalDateTime.now()).build();

        when(imageRepository.findById(12L)).thenReturn(Optional.of(img));

        assertThrows(AccessDeniedException.class, () -> imageService.deleteByIdForUser(12L, requester));
        verify(r2Client, never()).deleteObject((DeleteObjectRequest) any());
        verify(imageRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteByIdForUser_storageFailure_throwsRuntime() {
        User u = user(1L);
        Image img = Image.builder().id(13L).user(u).filename("users/1/images/baz.png").createdAt(LocalDateTime.now()).build();

        when(imageRepository.findById(13L)).thenReturn(Optional.of(img));

        UserService userService = mock(UserService.class);
        S3Client failingR2 = mock(S3Client.class);
        doThrow(new RuntimeException("S3 down")).when(failingR2).deleteObject(any(DeleteObjectRequest.class));
        R2Service r2Service = mock(R2Service.class);
        RestClient restClient = mock(RestClient.class);

        ImageService failingService = new ImageService(userService, failingR2, r2Service, restClient, imageRepository);

        assertThrows(RuntimeException.class, () -> failingService.deleteByIdForUser(13L, u));
        verify(imageRepository, never()).deleteById(anyLong());
    }

    @Test
    void generateFoodImageFromParams_maiBadRequest_usesDefaultErrorMessage() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        String maiErrorBody = """
                {"error":{"message":"Request blocked by content filters.","code":"content_filter"}}
                """;
        HttpClientErrorException badRequest = HttpClientErrorException.BadRequest.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                maiErrorBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        when(responseSpec.body(Map.class)).thenThrow(badRequest);

        FoodImageRequest request = new FoodImageRequest();
        request.setName("edible cube");
        request.setStyle("natural");
        request.setSize("1024x1024");
        request.setIngredients("tomato, basil, mozzarella");

        InappropriateRequestRefusalException ex = assertThrows(
                InappropriateRequestRefusalException.class,
                () -> imageService.generateFoodImageFromParams(request)
        );

        assertEquals("Sorry, I can only generate images of food.", ex.getMessage());
        verify(restClient, times(1)).post();
    }

    @Test
    void generateFoodImageFromParams_nonFoodKeywordInCourse_rejectedBeforeApiCall() {
        FoodImageRequest request = new FoodImageRequest();
        request.setName("test dish");
        request.setStyle("natural");
        request.setSize("1024x1024");
        request.setCourse("main with hammer garnish");
        request.setIngredients("tomato, basil, mozzarella");
        request.setDishType("pasta");

        InappropriateRequestRefusalException ex = assertThrows(
                InappropriateRequestRefusalException.class,
                () -> imageService.generateFoodImageFromParams(request)
        );

        assertEquals("Sorry, I can only generate images of food.", ex.getMessage());
        verifyNoInteractions(restClient);
    }
}
