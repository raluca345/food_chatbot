package org.ai.chatbot_backend.service;

import com.azure.core.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatModel chatModel;

    public String getResponse(String prompt) {
        try {
            return chatModel.call(prompt);
        }
        //a filter was triggered
        catch (HttpResponseException e) {
            return "Sorry, I can't help with that request";
        }
    }

    public String getResponseOptions(String prompt) {
        try {
            ChatResponse response = chatModel.call(
                    new Prompt(
                            prompt,
                            AzureOpenAiChatOptions.builder()
                                    .deploymentName("gpt-4.1-mini")
                                    .temperature(0.4)
                                    .build()
                    ));
            return response.getResult().getOutput().getText();
        }
        //a filter was triggered
        catch (HttpResponseException e) {
            return "Sorry, I can't help with that request";
        }
    }
}
