package org.ai.chatbot_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeResult {
    private String recipeMarkdown;
    private Long fileId;
    private String downloadMarkdown;

    public String toFullText() {
        String dm = downloadMarkdown == null ? "" : downloadMarkdown;
        if (dm.isEmpty()) return recipeMarkdown == null ? "" : recipeMarkdown;
        return (recipeMarkdown == null ? "" : recipeMarkdown) + "\n\nYou can download this recipe here: " + dm;
    }

    public String contentWithoutDownload() {
        String content = recipeMarkdown == null ? "" : recipeMarkdown;
        if (downloadMarkdown != null && !downloadMarkdown.isBlank()) {
            return content;
        }


        int idx = content.lastIndexOf("\n\nYou can download this recipe here:");
        if (idx != -1) {
            return content.substring(0, idx).trim();
        }
        int idx2 = content.lastIndexOf("\n\n[Download recipe]");
        if (idx2 != -1) {
            return content.substring(0, idx2).trim();
        }
        return content;
    }
}
