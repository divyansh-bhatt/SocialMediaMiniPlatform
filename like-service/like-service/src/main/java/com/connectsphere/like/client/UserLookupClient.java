package com.connectsphere.like.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * UserLookupClient for like-service.
 *
 * Two responsibilities:
 *  1. Resolve the owner (authorId) of the liked target so we know who to notify.
 *     - POST target  → GET /posts/internal/{postId}/author-id     (post-service)
 *     - COMMENT target → GET /comments/internal/{commentId}/author-id (comment-service)
 *
 *  2. Resolve userId → username for the notification message.
 *     → GET /auth/internal/username-by-id/{userId}               (auth-service)
 *
 * All calls return -1 / "Someone" on failure so a notification error never
 * breaks the actual like operation.
 */
@Component
public class UserLookupClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${post.service.url}")
    private String postServiceUrl;

    @Value("${comment.service.url}")
    private String commentServiceUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    /** Returns the authorId of the liked POST, or -1 on failure. */
    public int getPostAuthorId(int postId) {
        try {
            String url = postServiceUrl + "/posts/internal/" + postId + "/author-id";
            Integer id = restTemplate.getForObject(url, Integer.class);
            return id != null ? id : -1;
        } catch (Exception e) {
            System.err.println("[like-service] WARNING: could not fetch post author for postId="
                    + postId + ": " + e.getMessage());
            return -1;
        }
    }

    /** Returns the authorId of the liked COMMENT, or -1 on failure. */
    public int getCommentAuthorId(int commentId) {
        try {
            String url = commentServiceUrl + "/comments/internal/" + commentId + "/author-id";
            Integer id = restTemplate.getForObject(url, Integer.class);
            return id != null ? id : -1;
        } catch (Exception e) {
            System.err.println("[like-service] WARNING: could not fetch comment author for commentId="
                    + commentId + ": " + e.getMessage());
            return -1;
        }
    }

    /** Returns the username of a user, or "Someone" on failure. */
    public String getUsernameById(int userId) {
        try {
            String url = authServiceUrl + "/auth/internal/username-by-id/" + userId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object name = response.getBody().get("username");
                if (name instanceof String) return (String) name;
            }
        } catch (Exception e) {
            System.err.println("[like-service] WARNING: could not fetch username for userId="
                    + userId + ": " + e.getMessage());
        }
        return "Someone";
    }
}