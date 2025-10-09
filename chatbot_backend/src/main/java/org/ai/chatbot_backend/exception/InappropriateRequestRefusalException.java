package org.ai.chatbot_backend.exception;

public class InappropriateRequestRefusalException extends RuntimeException {
    public InappropriateRequestRefusalException(String message) {
        super(message);
    }
}

