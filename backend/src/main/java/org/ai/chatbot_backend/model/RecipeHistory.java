package org.ai.chatbot_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.ai.chatbot_backend.dto.RecipeHistoryDto;

@Entity
@Data
@Table(name = "recipe_history")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecipeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    @JsonIgnore
    private RecipeFile recipeFile;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public RecipeHistoryDto toDto() {
        Long fileId = null;
        if (this.recipeFile != null) {
            fileId = this.recipeFile.getId();
        }
        return new RecipeHistoryDto(
                this.id,
                this.title,
                this.content,
                fileId,
                this.createdAt
        );
    }
}
