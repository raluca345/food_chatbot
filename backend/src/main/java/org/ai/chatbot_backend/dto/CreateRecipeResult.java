package org.ai.chatbot_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a recipe creation request")
public class CreateRecipeResult {
    @Schema(
            description = "Recipe content in markdown format",
            example = "# Spaghetti Carbonara\n\n## Ingredients\n- 400g spaghetti"
    )
    private String recipeMarkdown;

    @Schema(
            description = "File ID for the downloadable recipe file",
            example = "42"
    )
    private Long fileId;

    @Schema(
            description = "Download link in markdown format",
            example = "[Download recipe](https://api.example.com/recipes/download/42)"
    )
    private String downloadMarkdown;

    // Jackson exposes this as JSON field fullText
    public String getFullText() {
        return toFullText();
    }

    public String toFullText() {
        String dm = downloadMarkdown == null ? "" : downloadMarkdown;
        if (dm.isEmpty()) return recipeMarkdown == null ? "" : recipeMarkdown;
        return (recipeMarkdown == null ? "" : recipeMarkdown) + "\n\nYou can download this recipe here: " + dm;
    }

    public String contentWithoutDownload() {
        String content = recipeMarkdown == null ? "" : recipeMarkdown;

        int idx = content.lastIndexOf("\n\nYou can download this recipe here:");
        if (idx != -1) {
            return content.substring(0, idx).trim();
        }
        int idx2 = content.lastIndexOf("\n\n[Download recipe]");
        if (idx2 != -1) {
            return content.substring(0, idx2).trim();
        }

        int idx3 = content.lastIndexOf("\n\nDownload link here");
        if (idx3 != -1) {
            return content.substring(0, idx3).trim();
        }


        String trimmed = content.trim();
        if (trimmed.endsWith("Download link here")) {
            int pos = content.lastIndexOf("Download link here");
            if (pos != -1) {
                return content.substring(0, pos).trim();
            }
        }

        return content;
    }
}
