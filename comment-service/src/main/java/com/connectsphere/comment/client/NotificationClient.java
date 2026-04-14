package com.connectsphere.comment.client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
/**
 * NotificationClient — fire-and-forget helper for comment-service.
 *
 * Every method builds the notification payload and POSTs it to
 * notification-service's internal endpoint (POST /notifications/internal/create).
 *
 * All calls are wrapped in try/catch so a notification failure NEVER
 * breaks the main business operation (adding a comment, liking, etc.).
 *
 * Notification types dispatched from comment-service:
 *   COMMENT — someone commented on a post you authored
 *   REPLY   — someone replied to your comment
 *   MENTION — someone @mentioned you in a comment
 */
@Component
public class NotificationClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private static final String INTERNAL_CREATE = "/notifications/internal/create";
    // Called when a top-level comment is added to a post.
    //   recipientId = post author
    //   actorId     = commenter
    //   targetId    = postId

    public void sendCommentNotification(int recipientId, int actorId,
                                        int postId, int commentId, String actorUsername) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("actorId", actorId);
        payload.put("type", "COMMENT");
        payload.put("message", actorUsername + " commented on your post.");
        payload.put("targetId", postId);
        payload.put("targetType", "POST");
        payload.put("deepLinkUrl", "/posts/" + postId + "#comment-" + commentId);
        sendSilently(payload);
    }

    // Called when a reply is added to an existing comment.
    //   recipientId = author of the parent comment
    //   actorId     = replier
    //   targetId    = parent commentId

    public void sendReplyNotification(int recipientId, int actorId,
                                      int postId, int parentCommentId,
                                      int replyCommentId, String actorUsername) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("actorId", actorId);
        payload.put("type", "REPLY");
        payload.put("message", actorUsername + " replied to your comment.");
        payload.put("targetId", parentCommentId);
        payload.put("targetType", "COMMENT");
        payload.put("deepLinkUrl", "/posts/" + postId + "#comment-" + replyCommentId);
        sendSilently(payload);
    }

    // Called once per @username token found in a comment's content.
    //   recipientId = the mentioned user's ID
    //   actorId     = comment author
    //   targetId    = commentId

    public void sendMentionNotification(int recipientId, int actorId,
                                        int postId, int commentId, String actorUsername) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("actorId", actorId);
        payload.put("type", "MENTION");
        payload.put("message", actorUsername + " mentioned you in a comment.");
        payload.put("targetId", commentId);
        payload.put("targetType", "COMMENT");
        payload.put("deepLinkUrl", "/posts/" + postId + "#comment-" + commentId);
        sendSilently(payload);
    }

    private void sendSilently(Map<String, Object> payload) {
        try {
            System.out.println("SENDING NOTIFICATION → " + payload);
            System.out.println("URL → " + notificationServiceUrl + "/notifications/internal/create");
            restTemplate.postForEntity(
                    notificationServiceUrl + INTERNAL_CREATE,
                    payload,
                    Void.class);
        } catch (Exception e) {
            System.err.println("[comment-service] WARNING: Could not send notification: "
                    + e.getMessage());
        }
    }
}
