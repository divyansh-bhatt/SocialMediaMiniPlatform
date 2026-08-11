package com.connectsphere.media.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Media — a single uploaded file (image or video).
 * Each Media record is linked to a post via linkedPostId.
 * When a post is deleted, its Media records are soft-deleted (isDeleted=true)
 * but retained in DB for 30 days for audit purposes (per NFR).
 *
 * mediaType: IMAGE | VIDEO
 */
@Entity
@Table(name = "media", indexes = {
        @Index(name = "idx_uploader", columnList = "uploader_id"),
        @Index(name = "idx_linked_post", columnList = "linked_post_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private int mediaId;

    /** The user who uploaded this file */
    @Column(name = "uploader_id", nullable = false)
    private int uploaderId;

    /** Cloudinary secure_url — the CDN-backed public URL */
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /** Cloudinary public_id — needed to delete from Cloudinary later */
    @Column(name = "public_id", length = 300)
    private String publicId;

    /** IMAGE or VIDEO */
    @Column(name = "media_type", length = 20)
    private String mediaType;

    /** File size in kilobytes */
    @Column(name = "size_kb")
    private long sizeKb;

    /** MIME type e.g. image/jpeg, video/mp4 */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** The post this media is attached to (null if standalone/story-linked) */
    @Column(name = "linked_post_id")
    private Integer linkedPostId;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    /** Soft-delete — true when the linked post is deleted */
    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
