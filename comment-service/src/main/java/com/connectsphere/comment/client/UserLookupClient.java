package com.connectsphere.comment.client;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * UserLookupClient — calls auth-service to resolve a username to a userId.
 *
 * Endpoint used: GET /auth/internal/user-by-username/{username}
 * (We will add this internal endpoint to auth-service below.)
 *
 * Returns -1 if the username does not exist or auth-service is unreachable.
 * The caller should skip notification for any -1 result.
 */
@Component
public class UserLookupClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    /**
     * Resolve @username → userId.
     * @return the integer userId, or -1 if not found / service unavailable.
     */
    public int getUserIdByUsername(String username) {
        try {
            String url = authServiceUrl + "/auth/internal/user-by-username/" + username;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object id = response.getBody().get("userId");
                if (id instanceof Integer) return (Integer) id;
                if (id instanceof Number) return ((Number) id).intValue();
            }
        } catch (HttpClientErrorException.NotFound e) {
            // Username doesn't exist — silently ignore
        } catch (Exception e) {
            System.err.println("[comment-service] WARNING: UserLookupClient failed for @"
                    + username + ": " + e.getMessage());
        }
        return -1;
    }

    /**
     * Resolve userId → username (used to build notification messages).
     * @return the username string, or "Someone" if not found.
     */
    public String getUsernameById(int userId) {
        try {
            String url = authServiceUrl + "/auth/internal/username-by-id/" + userId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object name = response.getBody().get("username");
                if (name instanceof String) return (String) name;
            }
        } catch (Exception e) {
            System.err.println("[comment-service] WARNING: UserLookupClient failed for userId="
                    + userId + ": " + e.getMessage());
        }
        return "Someone";
    }
}
