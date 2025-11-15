package org.ai.chatbot_backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public final class TestJsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestJsonUtils() {
    }

    /**
     * Safely serialize an object to a JSON string for embedding in tests.
     * Wraps checked exceptions in a RuntimeException for convenience in tests.
     */
    public static String toJson(Object value) {
        Objects.requireNonNull(value, "value");
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test value to JSON", e);
        }
    }
}

