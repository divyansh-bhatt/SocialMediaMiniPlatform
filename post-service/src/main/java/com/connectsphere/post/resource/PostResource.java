package com.connectsphere.post.resource;

import com.connectsphere.post.entity.Post;
import com.connectsphere.post.service.MediaService;
import com.connectsphere.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
public class PostResource {

    @Autowired
    private PostService postService;

    @Autowired
    private MediaService mediaService;

    // ─── POST /posts ──────────────────────────────────────────────────────────
    // Create a new post. JWT required — authorId extracted from token.
    // Body: { "content": "Hello world!", "visibility": "PUBLIC" }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post,
                                           HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        post.setAuthorId(userId); // Set author from JWT, not from request body
        Post created = postService.createPost(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── GET /posts/{postId} ──────────────────────────────────────────────────
    // Public: anyone can view a post by ID

    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPostById(@PathVariable int postId) {
        return postService.getPostById(postId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── GET /posts/user/{userId} ─────────────────────────────────────────────
    // Get all posts by a specific user

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Post>> getPostsByUser(@PathVariable int userId) {
        return ResponseEntity.ok(postService.getPostsByUser(userId));
    }

    // ─── GET /posts/feed ──────────────────────────────────────────────────────
    // Get personalised news feed. JWT required.
    // Internally calls follow-service to get followee IDs.

    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getFeed(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        return ResponseEntity.ok(postService.getFeedForUser(userId));
    }

    // ─── GET /posts/public ────────────────────────────────────────────────────
    // Browse all public posts (guest access)

    @GetMapping("/public")
    public ResponseEntity<List<Post>> getPublicPosts() {
        return ResponseEntity.ok(postService.searchPosts(""));
    }

    // ─── GET /posts/search?keyword=java ───────────────────────────────────────
    // Full-text search across public posts

    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(@RequestParam String keyword) {
        return ResponseEntity.ok(postService.searchPosts(keyword));
    }

    // ─── PUT /posts/{postId} ──────────────────────────────────────────────────
    // Update post content. Only the author can update.

    @PutMapping("/{postId}")
    public ResponseEntity<Post> updatePost(@PathVariable int postId,
                                           @RequestBody Post updatedPost,
                                           HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        Post existing = postService.getPostById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        // Ownership check — only the author can edit
        if (existing.getAuthorId() != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(postService.updatePost(postId, updatedPost));
    }

    // ─── DELETE /posts/{postId} ───────────────────────────────────────────────
    // Soft-delete a post. Only author or admin can delete.

    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, String>> deletePost(@PathVariable int postId,
                                                           HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        Post existing = postService.getPostById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        if (existing.getAuthorId() != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.deletePost(postId);
        return ResponseEntity.ok(Map.of("message", "Post deleted successfully."));
    }

    // ─── PUT /posts/{postId}/visibility ───────────────────────────────────────
    // Change post visibility. Body: { "visibility": "PRIVATE" }

    @PutMapping("/{postId}/visibility")
    public ResponseEntity<Map<String, String>> changeVisibility(@PathVariable int postId,
                                                                 @RequestBody Map<String, String> body,
                                                                 HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        Post existing = postService.getPostById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        if (existing.getAuthorId() != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.changeVisibility(postId, body.get("visibility"));
        return ResponseEntity.ok(Map.of("message", "Visibility updated."));
    }

    // ─── GET /posts/count/{userId} ────────────────────────────────────────────

    @GetMapping("/count/{userId}")
    public ResponseEntity<Integer> getPostCount(@PathVariable int userId) {
        return ResponseEntity.ok(postService.getPostCount(userId));
    }

    /**
     * GET /posts/admin/all
     * Get ALL posts including deleted ones — admin view.
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<Post>> getAllPostsAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(postService.getAllPostsAdmin());
    }

    /**
     * DELETE /posts/admin/{postId}
     * Admin hard-deletes any post regardless of author.
     */
    @DeleteMapping("/admin/{postId}")
    public ResponseEntity<Map<String, String>> adminDeletePost(@PathVariable int postId,
                                                               HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        postService.deletePost(postId);
        return ResponseEntity.ok(Map.of("message", "Post deleted by admin."));
    }

    // ─── INTERNAL ENDPOINTS (called by other microservices) ──────────────────
    // These are NOT for end users — only called by like-service, comment-service

    @PostMapping("/internal/{postId}/likes/increment")
    public ResponseEntity<Void> incrementLikes(@PathVariable int postId) {
        postService.incrementLikes(postId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/{postId}/likes/decrement")
    public ResponseEntity<Void> decrementLikes(@PathVariable int postId) {
        postService.decrementLikes(postId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/{postId}/comments/increment")
    public ResponseEntity<Void> incrementComments(@PathVariable int postId) {
        postService.incrementComments(postId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/{postId}/comments/decrement")
    public ResponseEntity<Void> decrementComments(@PathVariable int postId) {
        postService.decrementComments(postId);
        return ResponseEntity.ok().build();
    }
    // Called by comment-service: resolve which user authored a post so a
    // COMMENT notification is sent to the correct recipient.
    @GetMapping("/internal/{postId}/author-id")
    public ResponseEntity<Integer> getPostAuthorId(@PathVariable int postId) {
        return postService.getPostById(postId)
                .map(post -> ResponseEntity.ok(post.getAuthorId()))
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @PostMapping(value = "/with-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Post> createPostWithMedia(
            @RequestParam("content") String content,
            @RequestParam("visibility") String visibility,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) throws IOException {

        int userId = (int) request.getAttribute("userId");

        // Upload to Cloudinary
        List<String> mediaUrls = mediaService.uploadMultiple(files);

        // Create post
        Post post = new Post();
        post.setContent(content);
        post.setVisibility(visibility);
        post.setAuthorId(userId);
        post.setMediaUrls(mediaUrls);
        post.setPostType("MEDIA");

        Post created = postService.createPost(post);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
