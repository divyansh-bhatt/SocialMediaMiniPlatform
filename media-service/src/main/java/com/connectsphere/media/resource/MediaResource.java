package com.connectsphere.media.resource;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * MediaResource — REST API for media uploads and story management.
 *
 * Port: 8087
 *
 * Media endpoints:
 *   POST   /media/upload                  — upload a file (JWT required)
 *   POST   /media/upload/post/{postId}    — upload + link to a post (JWT required)
 *   GET    /media/post/{postId}           — get all media for a post (public)
 *   GET    /media/{mediaId}               — get one media record (public)
 *   DELETE /media/{mediaId}               — soft-delete a media record (owner only)
 *
 * Story endpoints:
 *   POST   /stories                        — create a story (JWT required)
 *   GET    /stories/user/{userId}          — get active stories by user (JWT required)
 *   GET    /stories/feed                   — stories from people you follow (JWT required)
 *   POST   /stories/{storyId}/view         — register a view (JWT required)
 *   DELETE /stories/{storyId}              — delete a story (owner only)
 */
@RestController
@RequestMapping
public class MediaResource {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${follow.service.url:http://localhost:8085}")
    private String followServiceUrl;

    /**
     * POST /media/upload
     * Upload a file without linking it to a post yet.
     * Useful when building the post UI before submission.
     * Returns the Media record including the Cloudinary URL.
     */
    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Media> uploadMedia(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        int userId = (int) request.getAttribute("userId");
        Media saved = mediaService.uploadMedia(file, userId, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST /media/upload/post/{postId}
     * Upload a file and immediately link it to an existing post.
     */
    @PostMapping(value = "/media/upload/post/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Media> uploadAndLinkToPost(
            @PathVariable int postId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        int userId = (int) request.getAttribute("userId");
        Media saved = mediaService.uploadMedia(file, userId, postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST /media/upload/post/{postId}/multiple
     * Upload multiple files and link them all to a post.
     */
    @PostMapping(value = "/media/upload/post/{postId}/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Media>> uploadMultiple(
            @PathVariable int postId,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) throws IOException {

        int userId = (int) request.getAttribute("userId");
        List<Media> saved = mediaService.uploadMultiple(files, userId, postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /media/post/{postId}
     * Get all media attached to a post. Public — no JWT needed.
     */
    @GetMapping("/media/post/{postId}")
    public ResponseEntity<List<Media>> getByPost(@PathVariable int postId) {
        return ResponseEntity.ok(mediaService.getMediaByPost(postId));
    }

    /**
     * GET /media/{mediaId}
     * Get one media record. Public.
     */
    @GetMapping("/media/{mediaId}")
    public ResponseEntity<Media> getById(@PathVariable int mediaId) {
        return mediaService.getMediaById(mediaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /media/{mediaId}
     * Soft-delete a media record. Only the uploader or ADMIN can delete.
     */
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @PathVariable int mediaId,
            HttpServletRequest request) throws IOException {

        int userId = (int) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        Media media = mediaService.getMediaById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        if (media.getUploaderId() != userId && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        mediaService.deleteMedia(mediaId);
        return ResponseEntity.ok(Map.of("message", "Media deleted."));
    }

    // STORY ENDPOINTS

    /**
     * POST /stories
     * Create a new story. JWT required.
     * Form fields:
     *   - file     (required) — the image or video file
     *   - caption  (optional) — text caption
     */
    @PostMapping(value = "/stories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Story> createStory(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            HttpServletRequest request) throws IOException {

        int userId = (int) request.getAttribute("userId");
        Story story = mediaService.createStory(file, userId, caption);
        return ResponseEntity.status(HttpStatus.CREATED).body(story);
    }

    /**
     * GET /stories/user/{userId}
     * Get all active stories by a specific user. JWT required.
     */
    @GetMapping("/stories/user/{userId}")
    public ResponseEntity<List<Story>> getStoriesByUser(@PathVariable int userId) {
        return ResponseEntity.ok(mediaService.getActiveStoriesByUser(userId));
    }

    /**
     * GET /stories/feed
     * Get active stories from people the requesting user follows.
     * Calls follow-service to get the list of followee IDs.
     * JWT required.
     */
    @GetMapping("/stories/feed")
    public ResponseEntity<List<Story>> getStoriesFeed(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");

        List<Integer> followingIds;
        try {
            String url = followServiceUrl + "/follows/" + userId + "/following-ids";
            ResponseEntity<List<Integer>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Integer>>() {});
            followingIds = response.getBody();
        } catch (Exception e) {
            System.err.println("[media-service] WARNING: follow-service unreachable: "
                    + e.getMessage());
            return ResponseEntity.ok(List.of());
        }

        if (followingIds == null || followingIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(mediaService.getActiveStoriesForFeed(followingIds));
    }

    /**
     * POST /stories/{storyId}/view
     * Register a view. JWT required.
     * Author viewing their own story is ignored (no self-view counting).
     */
    @PostMapping("/stories/{storyId}/view")
    public ResponseEntity<Map<String, String>> viewStory(
            @PathVariable int storyId,
            HttpServletRequest request) {

        int userId = (int) request.getAttribute("userId");
        mediaService.viewStory(storyId, userId);
        return ResponseEntity.ok(Map.of("message", "View registered."));
    }

    /**
     * DELETE /stories/{storyId}
     * Delete a story. Only the author can delete their story.
     */
    @DeleteMapping("/stories/{storyId}")
    public ResponseEntity<Map<String, String>> deleteStory(
            @PathVariable int storyId,
            HttpServletRequest request) throws IOException {

        int userId = (int) request.getAttribute("userId");
        mediaService.deleteStory(storyId, userId);
        return ResponseEntity.ok(Map.of("message", "Story deleted."));
    }

    // ─── GLOBAL EXCEPTION HANDLER ─────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIO(IOException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "File upload failed: " + ex.getMessage()));
    }
}
