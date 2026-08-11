package com.connectsphere.auth.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * SearchClient — auth-service calls this after register(), updateProfile(), deactivateAccount().
 *
 * All calls fire-and-forget in try/catch.
 *
 */
@Component
public class SearchClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${search.service.url:http://localhost:8088}")
    private String searchServiceUrl;

    /**
     * Index or update a user document after registration or profile update.
     */
    public void indexUser(int userId, String username, String fullName,
                          String bio, String profilePicUrl) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId",       userId);
            payload.put("username",     username);
            payload.put("fullName",     fullName     != null ? fullName     : "");
            payload.put("bio",          bio          != null ? bio          : "");
            payload.put("profilePicUrl", profilePicUrl != null ? profilePicUrl : "");

            restTemplate.postForEntity(
                    searchServiceUrl + "/search/internal/index/user",
                    payload,
                    Void.class);
        } catch (Exception e) {
            System.err.println("[auth-service] WARNING: SearchClient.indexUser failed for userId="
                    + userId + ": " + e.getMessage());
        }
    }

    /**
     * Remove a user from the search index on account deactivation.
     */
    public void removeUserIndex(int userId) {
        try {
            restTemplate.delete(searchServiceUrl + "/search/internal/index/user/" + userId);
        } catch (Exception e) {
            System.err.println("[auth-service] WARNING: SearchClient.removeUserIndex failed for userId="
                    + userId + ": " + e.getMessage());
        }
    }
}