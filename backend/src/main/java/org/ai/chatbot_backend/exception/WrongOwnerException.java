package org.ai.chatbot_backend.exception;

public class WrongOwnerException extends RuntimeException {
    public WrongOwnerException(String message) { super(message); }
}

