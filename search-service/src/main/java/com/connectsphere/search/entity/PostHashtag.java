package com.connectsphere.search.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PostHashtag — many-to-many join between posts and hashtags.
 *
 * One row per (postId, hashtagId) pair.
 * When a post is deleted/unindexed, all its PostHashtag rows are removed
 * and the corresponding Hashtag.postCount values are decremented.
 */
@Entity
@Table(name = "post_hashtags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "hashtag_id"}),
        indexes = {
                @Index(name = "idx_ph_post",    columnList = "post_id"),
                @Index(name = "idx_ph_hashtag", columnList = "hashtag_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "post_id", nullable = false)
    private int postId;

    @Column(name = "hashtag_id", nullable = false)
    private int hashtagId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
