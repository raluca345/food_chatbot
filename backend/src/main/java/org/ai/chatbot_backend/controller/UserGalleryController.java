package org.ai.chatbot_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ai.chatbot_backend.dto.ImageContent;
import org.ai.chatbot_backend.dto.ImagePageDto;
import org.ai.chatbot_backend.exception.ResourceNotFoundException;
import org.ai.chatbot_backend.model.User;
import org.ai.chatbot_backend.security.AuthHelper;
import org.ai.chatbot_backend.service.interfaces.IImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/users")
public class UserGalleryController {

    private final IImageService imageService;
    private final AuthHelper authHelper;

    @GetMapping("/me/images")
    public ResponseEntity<ImagePageDto> getMyImages(Authentication authentication,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "18") int pageSize) {

        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ImagePageDto pageDto = imageService.getImagesForUserPaged(user, page, pageSize);
        return ResponseEntity.ok(pageDto);
    }

    @GetMapping("/me/images/{imageId}/download")
    public ResponseEntity<Resource> downloadImage(Authentication authentication, @PathVariable("imageId") Long imageId) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            ImageContent content = imageService.loadImageContentForUser(imageId, user);
            MediaType mediaType = MediaType.parseMediaType(content.contentType());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + content.filename() + "\"")
                    .contentType(mediaType)
                    .body(content.resource());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            log.error("Failed to download image {}", imageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/me/images/{imageId}")
    public ResponseEntity<Void> deleteImage(Authentication authentication,
            @PathVariable long imageId) {
        User user = authHelper.getAuthenticatedUserOrNull(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            imageService.deleteByIdForUser(imageId, user);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }


}
