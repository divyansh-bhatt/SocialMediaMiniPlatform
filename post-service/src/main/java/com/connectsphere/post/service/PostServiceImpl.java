package com.connectsphere.post.service;

import com.connectsphere.post.client.SearchClient;
import com.connectsphere.post.entity.Post;
import com.connectsphere.post.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SearchClient searchClient;
    // follow-service base URL — injected from application.properties
    // When follow-service is built, it will run on port 8085
    @Value("${follow.service.url:http://localhost:8085}")
    private String followServiceUrl;

    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;

    private void enrichWithUsername(Post post) {
        try {
            String url = authServiceUrl + "/auth/internal/username-by-id/" + post.getAuthorId();
            Map<String, String> res = restTemplate.getForObject(url, Map.class);
            if (res != null) post.setAuthorUsername(res.get("username"));
        } catch (Exception e) {
            post.setAuthorUsername("user_" + post.getAuthorId());
        }
    }
    @Override
    public Post createPost(Post post) {
        // Set defaults
        if (post.getVisibility() == null || post.getVisibility().isBlank()) {
            post.setVisibility("PUBLIC");
        }
        if (post.getPostType() == null || post.getPostType().isBlank()) {
            post.setPostType(
                (post.getMediaUrls() != null && !post.getMediaUrls().isEmpty())
                    ? "MEDIA" : "TEXT"
            );
        }
        post.setDeleted(false);
        Post saved = postRepository.save(post);
        searchClient.indexPost(
                saved.getPostId(),
                saved.getContent(),
                saved.getAuthorId(),
                saved.getVisibility(),
                saved.getPostType());

        return saved;
    }

    @Override
    public Optional<Post> getPostById(int postId) {
        return postRepository.findByPostIdAndIsDeletedFalse(postId);
    }

    @Override
    public List<Post> getPostsByUser(int authorId) {
        return postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(authorId);
    }

    // KEY MICROSERVICE INTERACTION:
    // 1. Call follow-service → GET /follows/{userId}/following-ids → returns List<Integer>
    // 2. Use those IDs to query post DB for their posts

    @Override
    public List<Post> getFeedForUser(int userId) {
        List<Integer> followeeIds;

        try {
            // Inter-service REST call to follow-service
            String url = followServiceUrl + "/follows/" + userId + "/following-ids";
            ResponseEntity<List<Integer>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Integer>>() {}
            );
            followeeIds = response.getBody();
        } catch (Exception e) {
            // If follow-service is down, return empty feed gracefully (resilience)
            System.err.println("[post-service] WARNING: follow-service call failed: "
                    + e.getMessage() + ". Returning empty feed.");
            return List.of();
        }

        if (followeeIds == null || followeeIds.isEmpty()) {
            return List.of();
        }

        return postRepository.findFeedByUserIds(followeeIds);
    }

    @Override
    public Post updatePost(int postId, Post updatedPost) {
        Post existing = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        if (updatedPost.getContent() != null) {
            existing.setContent(updatedPost.getContent());
        }
        if (updatedPost.getMediaUrls() != null) {
            existing.setMediaUrls(updatedPost.getMediaUrls());
        }
        if (updatedPost.getVisibility() != null) {
            existing.setVisibility(updatedPost.getVisibility());
        }

        Post saved = postRepository.save(existing);
        searchClient.indexPost(
                saved.getPostId(),
                saved.getContent(),
                saved.getAuthorId(),
                saved.getVisibility(),
                saved.getPostType());

        return saved;
    }

    @Override
    public void deletePost(int postId) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        post.setDeleted(true);
        postRepository.save(post);
    }

    @Override
    public List<Post> searchPosts(String keyword) {
        return postRepository.searchByContent(keyword);
    }

    // These are called by like-service and comment-service via REST

    @Override
    public void incrementLikes(int postId) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementLikes(int postId) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        int current = post.getLikesCount();
        post.setLikesCount(Math.max(0, current - 1)); // Never go below 0
        postRepository.save(post);
    }

    @Override
    public void incrementComments(int postId) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);
    }

    @Override
    public void decrementComments(int postId) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);
    }

    @Override
    public void changeVisibility(int postId, String visibility) {
        // Validate visibility value
        List<String> allowed = Arrays.asList("PUBLIC", "FOLLOWERS_ONLY", "PRIVATE");
        if (!allowed.contains(visibility)) {
            throw new RuntimeException("Invalid visibility: " + visibility +
                ". Must be PUBLIC, FOLLOWERS_ONLY, or PRIVATE.");
        }
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        post.setVisibility(visibility);
        postRepository.save(post);
        searchClient.indexPost(
                post.getPostId(),
                post.getContent(),
                post.getAuthorId(),
                post.getVisibility(),
                post.getPostType());
    }

    @Override
    public int getPostCount(int authorId) {
        return postRepository.countByAuthorIdAndIsDeletedFalse(authorId);
    }
}
