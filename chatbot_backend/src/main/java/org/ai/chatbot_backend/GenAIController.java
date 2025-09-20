package org.ai.chatbot_backend;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.image.ImageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GenAIController {
    private final ChatService chatService;
    private final ImageService imageService;

    @GetMapping("ask")
    public String getResponse(@RequestParam String prompt) {
        return chatService.getResponse(prompt);
    }

    @GetMapping("ask-options")
    public String getResponseOptions(@RequestParam String prompt) {
        return chatService.getResponseOptions(prompt);
    }

    @GetMapping("generate-image")
    public String generateImage(@RequestParam String prompt,
                                @RequestParam String style,
                                @RequestParam int height,
                                @RequestParam int width) {
        ImageResponse imageResponse = imageService.generateImage(prompt, style, height, width);
        return imageResponse.getResult().getOutput().getUrl();
    }
}
