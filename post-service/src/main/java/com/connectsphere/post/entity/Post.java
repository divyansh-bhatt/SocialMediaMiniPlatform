package com.connectsphere.post.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private int postId;

    // References auth-service's user — stored as plain int (no FK across services)
    @Column(name = "author_id", nullable = false)
    private int authorId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Stored as comma-separated URLs in DB; converted to List in Java
    @ElementCollection
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url")
    private List<String> mediaUrls;

    // TEXT or MEDIA
    @Column(name = "post_type", length = 20)
    private String postType = "TEXT";

    // PUBLIC, FOLLOWERS_ONLY, PRIVATE
    @Column(name = "visibility", length = 20)
    private String visibility = "PUBLIC";

    // Denormalised counters — updated by like-service and comment-service calls
    @Column(name = "likes_count")
    private int likesCount = 0;

    @Column(name = "comments_count")
    private int commentsCount = 0;

    @Column(name = "shares_count")
    private int sharesCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft delete — post is hidden but not removed from DB
    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
