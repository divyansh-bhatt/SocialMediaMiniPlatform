package com.connectsphere.search.resource;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SearchResource — REST API for search and hashtag operations.
 *
 * Port: 8088
 *
 * Internal endpoints (no JWT — called by post-service and auth-service):
 *   POST   /search/internal/index                  — index a post
 *   DELETE /search/internal/index/{postId}         — remove a post from index
 *   POST   /search/internal/index/user             — index a user
 *   DELETE /search/internal/index/user/{userId}    — remove user from index
 *
 * Public search endpoints (no JWT required):
 *   GET    /search/posts?keyword=spring             — full-text post search
 *   GET    /search/users?query=alice                — full-text user search
 *   GET    /search/hashtags?query=java              — hashtag autocomplete
 *   GET    /hashtags/trending                       — top trending hashtags
 *   GET    /hashtags/{tag}/posts                    — posts by hashtag
 *   GET    /hashtags/{tag}/count                    — post count for a hashtag
 *   GET    /hashtags/post/{postId}                  — hashtags on a post
 */
@RestController
public class SearchResource {

    @Autowired
    private SearchService searchService;

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERNAL ENDPOINTS — called by other services, no JWT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /search/internal/index
     * Called by post-service after createPost() and updatePost().
     * Body: { "postId":1, "content":"Hello #java world", "authorId":2,
     *         "visibility":"PUBLIC", "postType":"TEXT" }
     */
    @PostMapping("/search/internal/index")
    public ResponseEntity<Map<String, String>> indexPost(@RequestBody Map<String, Object> body) {
        int postId     = (int) body.get("postId");
        String content = (String) body.get("content");
        int authorId   = (int) body.get("authorId");
        String visibility = (String) body.getOrDefault("visibility", "PUBLIC");
        String postType   = (String) body.getOrDefault("postType", "TEXT");

        searchService.indexPost(postId, content, authorId, visibility, postType);
        return ResponseEntity.ok(Map.of("message", "Post indexed."));
    }

    /**
     * DELETE /search/internal/index/{postId}
     * Called by post-service after deletePost().
     */
    @DeleteMapping("/search/internal/index/{postId}")
    public ResponseEntity<Map<String, String>> removePostIndex(@PathVariable int postId) {
        searchService.removePostIndex(postId);
        return ResponseEntity.ok(Map.of("message", "Post removed from index."));
    }

    /**
     * POST /search/internal/index/user
     * Called by auth-service after register() and updateProfile().
     * Body: { "userId":1, "username":"alice", "fullName":"Alice Smith",
     *         "bio":"...", "profilePicUrl":"..." }
     */
    @PostMapping("/search/internal/index/user")
    public ResponseEntity<Map<String, String>> indexUser(@RequestBody Map<String, Object> body) {
        int userId          = (int) body.get("userId");
        String username     = (String) body.get("username");
        String fullName     = (String) body.getOrDefault("fullName", "");
        String bio          = (String) body.getOrDefault("bio", "");
        String profilePicUrl = (String) body.getOrDefault("profilePicUrl", "");
        System.out.println("🔥 RECEIVED USER INDEX REQUEST: " + userId);
        searchService.indexUser(userId, username, fullName, bio, profilePicUrl);
        return ResponseEntity.ok(Map.of("message", "User indexed."));
    }

    /**
     * DELETE /search/internal/index/user/{userId}
     * Called by auth-service after deactivateAccount().
     */
    @DeleteMapping("/search/internal/index/user/{userId}")
    public ResponseEntity<Map<String, String>> removeUserIndex(@PathVariable int userId) {
        searchService.removeUserIndex(userId);
        return ResponseEntity.ok(Map.of("message", "User removed from index."));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC SEARCH ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /search/posts?keyword=springboot
     * Returns list of postIds matching the keyword.
     * Client then fetches full post objects from post-service.
     */
    @GetMapping("/search/posts")
    public ResponseEntity<List<Integer>> searchPosts(@RequestParam String keyword) {
        return ResponseEntity.ok(searchService.searchPosts(keyword));
    }

    /**
     * GET /search/users?query=alice
     * Returns list of userIds matching the query.
     * Client fetches full user profiles from auth-service.
     */
    @GetMapping("/search/users")
    public ResponseEntity<List<Integer>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchUsers(query));
    }

    /**
     * GET /search/hashtags?query=jav
     * Hashtag autocomplete — partial match, returns Hashtag objects.
     */
    @GetMapping("/search/hashtags")
    public ResponseEntity<List<Hashtag>> searchHashtags(@RequestParam String query) {
        return ResponseEntity.ok(searchService.searchHashtags(query));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HASHTAG ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /hashtags/trending?limit=20
     * Top N trending hashtags. Default limit = 20.
     */
    @GetMapping("/hashtags/trending")
    public ResponseEntity<List<Hashtag>> getTrending(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.getTrendingHashtags(limit));
    }

    /**
     * GET /hashtags/{tag}/posts
     * All postIds that use this hashtag. Tag can include or omit #.
     */
    @GetMapping("/hashtags/{tag}/posts")
    public ResponseEntity<List<Integer>> getPostsByHashtag(@PathVariable String tag) {
        return ResponseEntity.ok(searchService.getPostsByHashtag(tag));
    }

    /**
     * GET /hashtags/{tag}/count
     * Post count for a hashtag.
     */
    @GetMapping("/hashtags/{tag}/count")
    public ResponseEntity<Integer> getHashtagCount(@PathVariable String tag) {
        return ResponseEntity.ok(searchService.getHashtagCount(tag));
    }

    /**
     * GET /hashtags/post/{postId}
     * All hashtags attached to a specific post.
     */
    @GetMapping("/hashtags/post/{postId}")
    public ResponseEntity<List<Hashtag>> getHashtagsForPost(@PathVariable int postId) {
        return ResponseEntity.ok(searchService.getHashtagsForPost(postId));
    }

    // ─── GLOBAL EXCEPTION HANDLER ─────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
