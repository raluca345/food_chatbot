package org.ai.chatbot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeHistoryDto {
    private Long id;
    private String title;
    private String content;
    private Long fileId;
    private LocalDateTime createdAt;
}

