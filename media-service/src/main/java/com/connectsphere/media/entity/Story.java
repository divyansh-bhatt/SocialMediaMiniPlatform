package com.connectsphere.media.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Story — an ephemeral 24-hour media post visible to a user's followers.
 *
 * expiresAt = createdAt + 24 hours — set automatically in @PrePersist.
 * isActive = false once the scheduled job marks it as expired.
 * viewsCount is incremented atomically each time another user views the story.
 *
 * mediaType: IMAGE | VIDEO
 */
@Entity
@Table(name = "stories", indexes = {
        @Index(name = "idx_story_author", columnList = "author_id"),
        @Index(name = "idx_story_active", columnList = "is_active, expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_id")
    private int storyId;

    /** The user who created this story */
    @Column(name = "author_id", nullable = false)
    private int authorId;

    /** Cloudinary URL of the story image or video */
    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    /** Cloudinary public_id — needed to delete from Cloudinary on story expiry */
    @Column(name = "public_id", length = 300)
    private String publicId;

    /** Optional caption text */
    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    /** IMAGE or VIDEO */
    @Column(name = "media_type", length = 20)
    private String mediaType;

    /** Number of unique views — incremented each time a follower opens the story */
    @Column(name = "views_count")
    private int viewsCount = 0;

    /** Automatically set to createdAt + 24 hours in @PrePersist */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * isActive = true while story is live.
     * The scheduled job (StoryExpiryScheduler) sets this to false when expiresAt has passed.
     */
    @Column(name = "is_active")
    private boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(24);  // 24-hour expiry
        this.isActive = true;
    }
}
