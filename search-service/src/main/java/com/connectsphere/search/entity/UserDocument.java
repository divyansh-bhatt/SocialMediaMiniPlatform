package com.connectsphere.search.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * UserDocument — Elasticsearch document stored in the "users" index.
 *
 * Mirrors user data from auth-service for fast full-text search by
 * username and fullName. Indexed when users register or update profile.
 *
 * auth-service calls POST /search/internal/index/user on register/profile-update.
 */
@Document(indexName = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    @Id
    private String id;          // stores userId as String

    @Field(type = FieldType.Integer)
    private int userId;

    /** Searchable — partial match on username */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String username;

    /** Searchable — partial match on full name */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String fullName;

    @Field(type = FieldType.Keyword)
    private String profilePicUrl;

    @Field(type = FieldType.Text)
    private String bio;
}
