package org.ai.chatbot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeHistoryPageDto {
    private List<RecipeHistoryDto> items;
    private int total;
}

