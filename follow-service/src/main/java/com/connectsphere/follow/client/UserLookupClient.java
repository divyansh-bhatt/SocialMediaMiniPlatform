package com.connectsphere.follow.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * UserLookupClient for follow-service.
 */
@Component
public class UserLookupClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public String getUsernameById(int userId) {
        try {
            String url = authServiceUrl + "/auth/internal/username-by-id/" + userId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object name = response.getBody().get("username");
                if (name instanceof String) return (String) name;
            }
        } catch (Exception e) {
            System.err.println("[follow-service] WARNING: could not fetch username for userId="
                    + userId + ": " + e.getMessage());
        }
        return "Someone";
    }
}
