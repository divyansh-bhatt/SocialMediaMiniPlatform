package com.connectsphere.like.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "likes",
    uniqueConstraints = {
        // KEY CONSTRAINT: one reaction per user per target — enforced at DB level
        // Prevents the duplicate-like problem that comment-service had
        @UniqueConstraint(columnNames = {"user_id", "target_id", "target_type"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private int likeId;

    // Who reacted
    @Column(name = "user_id", nullable = false)
    private int userId;

    // What they reacted to (postId or commentId)
    @Column(name = "target_id", nullable = false)
    private int targetId;

    // POST or COMMENT — polymorphic: one table handles both
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    // LIKE, LOVE, HAHA, WOW, SAD, ANGRY
    @Column(name = "reaction_type", nullable = false, length = 20)
    private String reactionType = "LIKE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
