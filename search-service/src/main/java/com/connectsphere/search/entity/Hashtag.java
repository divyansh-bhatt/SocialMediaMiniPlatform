package com.connectsphere.search.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Hashtag — persisted in MySQL.
 *
 * Each unique #tag gets one row. postCount is incremented each time
 * a post containing this tag is indexed, and decremented when removed.
 * lastUsedAt is updated on every upsert — used for trending computation.
 *
 * The tag field stores the hashtag WITHOUT the # symbol
 * e.g. content "#java" → tag = "java"
 */
@Entity
@Table(name = "hashtags", indexes = {
        @Index(name = "idx_tag_unique", columnList = "tag", unique = true),
        @Index(name = "idx_post_count", columnList = "post_count")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id")
    private int hashtagId;

    /** The tag text without '#', e.g. "java", "springboot". Unique. */
    @Column(name = "tag", nullable = false, unique = true, length = 100)
    private String tag;

    /** How many active posts use this hashtag — used for trending ranking */
    @Column(name = "post_count")
    private int postCount = 0;

    /** Updated every time a post uses this tag */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
