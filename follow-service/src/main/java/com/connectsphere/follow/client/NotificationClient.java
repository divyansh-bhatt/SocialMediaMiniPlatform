package com.connectsphere.follow.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * NotificationClient for follow-service.
 *
 * Dispatches a FOLLOW notification when a user follows another user.
 * No notification is sent on unfollow (consistent with major platforms).
 */
@Component
public class NotificationClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private static final String INTERNAL_CREATE = "/notifications/internal/create";

    /**
     * Notify the followee that someone started following them.
     *
     * @param followeeId    the user being followed (recipient)
     * @param followerId    the user doing the following (actor)
     * @param followerUsername human-readable username for the message
     */
    public void sendFollowNotification(int followeeId, int followerId, String followerUsername) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientId", followeeId);
        payload.put("actorId", followerId);
        payload.put("type", "FOLLOW");
        payload.put("message", followerUsername + " started following you.");
        payload.put("targetId", followerId);       // deep-link to follower's profile
        payload.put("targetType", "USER");
        payload.put("deepLinkUrl", "/profile/" + followerId);
        sendSilently(payload);
    }

    private void sendSilently(Map<String, Object> payload) {
        try {
            restTemplate.postForEntity(
                    notificationServiceUrl + INTERNAL_CREATE,
                    payload,
                    Void.class);
        } catch (Exception e) {
            System.err.println("[follow-service] WARNING: Could not send FOLLOW notification: "
                    + e.getMessage());
        }
    }
}
