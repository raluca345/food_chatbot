package org.ai.chatbot_backend.service.interfaces;

import org.ai.chatbot_backend.model.Conversation;
import org.ai.chatbot_backend.model.Message;

public interface IMessageService {
    Message createUserMessage(String content, Conversation conversation);

    Message createAssistantMessage(String assistantReply, Conversation conversation);
}
