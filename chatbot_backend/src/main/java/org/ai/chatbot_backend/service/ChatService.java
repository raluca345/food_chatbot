package org.ai.chatbot_backend.service;

import com.azure.core.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import org.ai.chatbot_backend.exception.InappropriateRequestRefusalException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatModel chatModel;

    private static final String SYSTEM_PROMPT =
            "You are a helpful assistant that only answers questions about food, recipes, ingredients, and cooking. " +
                    "If the question is not about food, politely respond:" +
                    "'Sorry, I can only answer questions about food.'";

    public String getResponse(String userPrompt) {
        String fullPrompt = SYSTEM_PROMPT + "\nUser: " + userPrompt;
        try {
            return chatModel.call(fullPrompt);
        }
        //an inappropriate content filter was triggered
        catch (HttpResponseException e) {
            throw new InappropriateRequestRefusalException("Sorry, I can't help with that request.");
        }
    }
}
