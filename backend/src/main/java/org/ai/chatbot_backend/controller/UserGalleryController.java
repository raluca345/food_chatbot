package org.ai.chatbot_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.dto.ImageContent;
import org.ai.chatbot_backend.dto.ImageDto;
import org.ai.chatbot_backend.dto.PageResult;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.interfaces.IImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "4. User Gallery", description = "Authenticated user's generated image gallery")
public class UserGalleryController {

    private final IImageService imageService;
    private final AuthHelper authHelper;

    @Operation(
            summary = "Get image gallery",
            description = "Returns paged gallery images for the authenticated user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gallery returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me/images")
    public ResponseEntity<PageResult<ImageDto>> getMyImages(Authentication authentication,
                                        @Parameter(description = "1-based page number", example = "1")
                                        @RequestParam(defaultValue = "1") int page,
                                        @Parameter(description = "Page size", example = "18")
                                        @RequestParam(defaultValue = "18") int pageSize) {

        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        PageResult<ImageDto> pageDto = imageService.getImages(user, page, pageSize);
        return ResponseEntity.ok(pageDto);
    }

    @Operation(
            summary = "Download image",
            description = "Downloads an image by id from the authenticated user's gallery."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image downloaded"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Image not found"),
            @ApiResponse(responseCode = "500", description = "Download failed")
    })
    @GetMapping("/me/images/{imageId}/download")
    public ResponseEntity<Resource> downloadImage(
            Authentication authentication,
            @Parameter(description = "Image id", example = "17")
            @PathVariable Long imageId) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        ImageContent content = imageService.loadImageContentForUser(imageId, user);
        MediaType mediaType = MediaType.parseMediaType(content.contentType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + content.filename() + "\"")
                .contentType(mediaType)
                .body(content.resource());
    }

    @Operation(
            summary = "Delete image from gallery",
            description = "Deletes an image from the authenticated user's gallery."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Image deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Image not found")
    })
    @DeleteMapping("/me/images/{imageId}")
    public ResponseEntity<?> deleteImage(Authentication authentication,
            @Parameter(description = "Image id", example = "17")
            @PathVariable long imageId) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        imageService.deleteByIdForUser(imageId, user);
        return ResponseEntity.noContent().build();
    }


}
