package org.ai.chatbot_backend.dto;

import org.springframework.core.io.Resource;

public record ImageContent(Resource resource, String filename, String contentType) {}
