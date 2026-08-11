package com.connectsphere.post.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * SearchClient — post-service calls this after createPost(), updatePost(), deletePost().
 *
 * All calls are fire-and-forget wrapped in try/catch.
 * A search-service failure NEVER breaks the main post operation.
 *
 * Endpoints called:
 *   POST   /search/internal/index          — on create/update
 *   DELETE /search/internal/index/{postId} — on delete
 */
@Component
public class SearchClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${search.service.url:http://localhost:8088}")
    private String searchServiceUrl;

    /**
     * Index or re-index a post after create/update.
     * Only PUBLIC posts appear in search results — search-service handles filtering.
     */
    public void indexPost(int postId, String content, int authorId,
                          String visibility, String postType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("postId",     postId);
            payload.put("content",    content != null ? content : "");
            payload.put("authorId",   authorId);
            payload.put("visibility", visibility != null ? visibility : "PUBLIC");
            payload.put("postType",   postType   != null ? postType   : "TEXT");

            restTemplate.postForEntity(
                    searchServiceUrl + "/search/internal/index",
                    payload,
                    Void.class);
        } catch (Exception e) {
            System.err.println("[post-service] WARNING: SearchClient.indexPost failed for postId="
                    + postId + ": " + e.getMessage());
        }
    }

    /**
     * Remove a post from the search index on delete.
     */
    public void removePostIndex(int postId) {
        try {
            restTemplate.delete(searchServiceUrl + "/search/internal/index/" + postId);
        } catch (Exception e) {
            System.err.println("[post-service] WARNING: SearchClient.removePostIndex failed for postId="
                    + postId + ": " + e.getMessage());
        }
    }
}
