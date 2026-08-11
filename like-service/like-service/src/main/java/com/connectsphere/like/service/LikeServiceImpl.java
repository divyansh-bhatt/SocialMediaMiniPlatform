package com.connectsphere.like.service;

import com.connectsphere.like.client.NotificationClient;
import com.connectsphere.like.client.UserLookupClient;
import com.connectsphere.like.entity.Like;
import com.connectsphere.like.repository.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private UserLookupClient userLookupClient;
    @Value("${post.service.url}")
    private String postServiceUrl;

    @Value("${comment.service.url}")
    private String commentServiceUrl;

    // Valid reaction types
    private static final List<String> VALID_REACTIONS =
        Arrays.asList("LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY");

    // Valid target types
    private static final List<String> VALID_TARGET_TYPES =
        Arrays.asList("POST", "COMMENT");
    // One reaction per user per target enforced by:
    //   1. existsByUserIdAndTargetId check (friendly error message)
    //   2. DB unique constraint (hard guarantee even under race conditions)

    @Override
    @Transactional
    public Like likeTarget(Like like) {
        validateReactionType(like.getReactionType());
        validateTargetType(like.getTargetType());

        // Check if user already reacted to this target
        if (likeRepository.existsByUserIdAndTargetIdAndTargetType(
                like.getUserId(), like.getTargetId(), like.getTargetType())) {
            throw new RuntimeException(
                "You have already reacted to this " + like.getTargetType().toLowerCase() +
                ". Use PUT /likes/reaction to change your reaction.");
        }

        Like saved;
        try {
            saved = likeRepository.save(like);
        } catch (DataIntegrityViolationException e) {
            // Catches race-condition duplicate inserts at DB level
            throw new RuntimeException("Duplicate reaction blocked.");
        }

        // Notify the appropriate service to increment its counter
        notifyCounterIncrement(like.getTargetId(), like.getTargetType());
        sendLikeNotification(saved);
        return saved;
    }

    @Override
    @Transactional
    public void unlikeTarget(int userId, int targetId, String targetType) {
        validateTargetType(targetType);

        if (!likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)) {
            throw new RuntimeException("You have not reacted to this " + targetType.toLowerCase());
        }

        likeRepository.deleteByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
        notifyCounterDecrement(targetId, targetType);
    }

    @Override
    public boolean hasLiked(int userId, int targetId, String targetType) {
        return likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
    }


    @Override
    public List<Like> getLikesByTarget(int targetId, String targetType) {
        validateTargetType(targetType);
        return likeRepository.findByTargetIdAndTargetType(targetId, targetType);
    }

    @Override
    public List<Like> getLikesByUser(int userId) {
        return likeRepository.findByUserId(userId);
    }


    @Override
    public int getLikeCount(int targetId, String targetType) {
        validateTargetType(targetType);
        return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
    }

    @Override
    public int getLikeCountByType(int targetId, String targetType, String reactionType) {
        validateTargetType(targetType);
        validateReactionType(reactionType);
        return likeRepository.countByTargetIdAndTargetTypeAndReactionType(
            targetId, targetType, reactionType);
    }

    // Returns map like: { "LIKE": 42, "LOVE": 7, "HAHA": 2 }
    // Only includes reaction types that have at least one reaction (no zero entries)

    @Override
    public Map<String, Long> getReactionSummary(int targetId, String targetType) {
        validateTargetType(targetType);
        List<Object[]> raw = likeRepository.getReactionSummaryRaw(targetId, targetType);
        Map<String, Long> summary = new LinkedHashMap<>();
        for (Object[] row : raw) {
            summary.put((String) row[0], (Long) row[1]);
        }
        return summary;
    }

    // Changes an existing reaction type without touching the counter
    // (count stays the same — just the emoji type changes)

    @Override
    @Transactional
    public Like changeReaction(int userId, int targetId, String targetType, String newReactionType) {
        validateTargetType(targetType);
        validateReactionType(newReactionType);

        Like existing = likeRepository
            .findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
            .orElseThrow(() -> new RuntimeException(
                "No existing reaction found. Use POST /likes to react first."));

        existing.setReactionType(newReactionType);
        return likeRepository.save(existing);
    }
    // Resolve the owner of the liked entity, then dispatch a LIKE notification.

    private void sendLikeNotification(Like like) {
        try {
            int recipientId;
            if ("POST".equals(like.getTargetType())) {
                recipientId = fetchPostAuthorId(like.getTargetId());
                System.out.println("Post author fetched: " + recipientId);
            } else {
                recipientId = fetchCommentAuthorId(like.getTargetId());
                System.out.println("Comment author fetched: " + recipientId);
            }
            System.out.println("Liker userId: " + like.getUserId());
            if (recipientId == -1 || recipientId == like.getUserId()) {
                // Can't resolve owner, or user liked their own content — skip
                System.out.println("recipientId is -1 → skipping or self skipping");
                return;
            }

            String actorUsername = userLookupClient.getUsernameById(like.getUserId());
            System.out.println("Actor username: " + actorUsername);

            System.out.println("CALLING NOTIFICATION CLIENT");
            notificationClient.sendLikeNotification(
                    recipientId, like.getUserId(),
                    like.getTargetId(), like.getTargetType(),
                    actorUsername);
        } catch (Exception e) {
            System.err.println("[like-service] WARNING: sendLikeNotification failed: "
                    + e.getMessage());
        }
    }

    private void validateReactionType(String reactionType) {
        if (!VALID_REACTIONS.contains(reactionType)) {
            throw new RuntimeException(
                "Invalid reaction type: " + reactionType +
                ". Must be one of: LIKE, LOVE, HAHA, WOW, SAD, ANGRY");
        }
    }

    private void validateTargetType(String targetType) {
        if (!VALID_TARGET_TYPES.contains(targetType)) {
            throw new RuntimeException(
                "Invalid target type: " + targetType + ". Must be POST or COMMENT");
        }
    }

    // When user likes a POST   → call post-service    to increment likesCount
    // When user likes a COMMENT → call comment-service to increment likesCount
    // Same in reverse for unlike

    private void notifyCounterIncrement(int targetId, String targetType) {
        try {
            if ("POST".equals(targetType)) {
                restTemplate.postForEntity(
                    postServiceUrl + "/posts/internal/" + targetId + "/likes/increment",
                    null, Void.class);
            } else if ("COMMENT".equals(targetType)) {
                restTemplate.postForEntity(
                    commentServiceUrl + "/comments/internal/" + targetId + "/like/increment",
                    null, Void.class);
            }
        } catch (Exception e) {
            System.err.println("[like-service] WARNING: counter increment failed for "
                + targetType + " id=" + targetId + ": " + e.getMessage());
        }
    }

    private void notifyCounterDecrement(int targetId, String targetType) {
        try {
            if ("POST".equals(targetType)) {
                restTemplate.postForEntity(
                    postServiceUrl + "/posts/internal/" + targetId + "/likes/decrement",
                    null, Void.class);
            } else if ("COMMENT".equals(targetType)) {
                restTemplate.postForEntity(
                    commentServiceUrl + "/comments/internal/" + targetId + "/likes/decrement",
                    null, Void.class);
            }
        } catch (Exception e) {
            System.err.println("[like-service] WARNING: counter decrement failed for "
                + targetType + " id=" + targetId + ": " + e.getMessage());
        }
    }
    private int fetchPostAuthorId(int postId) {
        try {
            String url = postServiceUrl + "/posts/internal/" + postId + "/author-id";
            Integer authorId = restTemplate.getForObject(url, Integer.class);
            return authorId != null ? authorId : -1;
        } catch (Exception e) {
            System.err.println("[like-service] ERROR fetching post author: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    private int fetchCommentAuthorId(int commentId) {
        try {
            String url = commentServiceUrl + "/comments/internal/" + commentId + "/author-id";
            Integer authorId = restTemplate.getForObject(url, Integer.class);
            return authorId != null ? authorId : -1;
        } catch (Exception e) {
            System.err.println("[like-service] ERROR fetching comment author: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
