package com.connectsphere.search.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PostDocument — Elasticsearch document stored in the "posts" index.
 *
 * This is NOT a MySQL entity. It mirrors the post data from post-service
 * but lives in Elasticsearch for full-text search.
 *
 * Indexed fields:
 *   - content (TEXT)    → full-text search with standard analyzer
 *   - authorId (int)    → filter by author
 *   - visibility        → filter — only PUBLIC posts are searchable
 *   - hashtags          → list of tags on this post (keyword for exact match)
 *   - createdAt         → sort by recency
 *
 * When a post is created/updated in post-service, it calls search-service's
 * internal endpoint POST /search/internal/index to add/update this document.
 * When deleted, it calls DELETE /search/internal/index/{postId}.
 */
@Document(indexName = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {

    @Id
    private String id;          // stores postId as String (ES requirement)

    @Field(type = FieldType.Integer)
    private int postId;

    @Field(type = FieldType.Integer)
    private int authorId;

    /** Full-text searchable field — analyzed with standard tokenizer */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String content;

    /** Only PUBLIC posts are indexed */
    @Field(type = FieldType.Keyword)
    private String visibility;

    @Field(type = FieldType.Keyword)
    private String postType;

    /** List of hashtag strings on this post (without #) */
    @Field(type = FieldType.Keyword)
    private List<String> hashtags;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
}
