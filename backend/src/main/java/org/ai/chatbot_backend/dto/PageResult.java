package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated result set")
public class PageResult<T> {
    @Schema(
            description = "List of items",
            example = "[]"
    )
    private List<T> items;

    @Schema(
            description = "Total number of items available",
            example = "42"
    )
    private long total;
}

