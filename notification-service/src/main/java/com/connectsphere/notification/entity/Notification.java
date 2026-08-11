package com.connectsphere.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single in-app notification.
 *
 * type values: LIKE | COMMENT | REPLY | FOLLOW | MENTION
 *   LIKE    — someone liked your post or comment
 *   COMMENT — someone commented on your post
 *   REPLY   — someone replied to your comment
 *   FOLLOW  — someone followed you
 *   MENTION — someone @mentioned you in a post or comment
 *
 * targetType values: POST | COMMENT (what the notification is about)
 *
 * deepLinkUrl — frontend uses this to navigate directly to the relevant content
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient", columnList = "recipient_id"),
        @Index(name = "idx_recipient_read", columnList = "recipient_id, is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private int notificationId;

    /** The user who should see this notification */
    @Column(name = "recipient_id", nullable = false)
    private int recipientId;

    /** The user who triggered the event (liked, commented, followed, mentioned) */
    @Column(name = "actor_id", nullable = false)
    private int actorId;

    /** Notification category: LIKE | COMMENT | REPLY | FOLLOW | MENTION */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /** Human-readable message, e.g. "john liked your post" */
    @Column(name = "message", nullable = false)
    private String message;

    /** ID of the post or comment this notification points to */
    @Column(name = "target_id")
    private Integer targetId;

    /** POST or COMMENT */
    @Column(name = "target_type", length = 20)
    private String targetType;

    /** Frontend deep-link, e.g. /posts/42 or /posts/42#comment-7 */
    @Column(name = "deep_link_url")
    private String deepLinkUrl;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
