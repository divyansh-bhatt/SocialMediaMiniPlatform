package com.connectsphere.like.client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
/**
 * NotificationClient — fire-and-forget helper for like-service.
 *
 * Notification types dispatched from like-service:
 *   LIKE — someone liked your post or comment
 *
 * The actorUsername is fetched by calling auth-service before this client
 * is invoked (see LikeServiceImpl).
 */
@Component
public class NotificationClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private static final String INTERNAL_CREATE = "/notifications/internal/create";

    // ─── LIKE NOTIFICATION ────────────────────────────────────────────────────
    // targetType is "POST" or "COMMENT"

    public void sendLikeNotification(int recipientId, int actorId,
                                     int targetId, String targetType,
                                     String actorUsername) {
        String entityLabel = "POST".equals(targetType) ? "post" : "comment";
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", recipientId);
        payload.put("actorId", actorId);
        payload.put("type", "LIKE");
        payload.put("message", actorUsername + " liked your " + entityLabel + ".");
        payload.put("targetId", targetId);
        payload.put("targetType", targetType);
        payload.put("deepLinkUrl", "/posts/" + targetId);   // post-service resolves the actual URL
        sendSilently(payload);
    }

    private void sendSilently(Map<String, Object> payload) {
        try {
            restTemplate.postForEntity(
                    notificationServiceUrl + INTERNAL_CREATE,
                    payload,
                    Void.class);
        } catch (Exception e) {
            System.err.println("[like-service] WARNING: Could not send notification: "
                    + e.getMessage());
        }
    }
}
