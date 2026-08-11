package com.connectsphere.auth.service;

import com.connectsphere.auth.client.SearchClient;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.jms.DeactivationEvent;
import com.connectsphere.auth.jms.DeactivationProducer;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private SearchClient searchClient;

    @Autowired
    private DeactivationProducer deactivationProducer;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Override
    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered: " + user.getEmail());
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already taken: " + user.getUsername());
        }

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        if (user.getProvider() == null || user.getProvider().isBlank()) {
            user.setProvider("LOCAL");
        }
        user.setActive(true);
        User saved = userRepository.save(user);
        searchClient.indexUser(
                saved.getUserId(),
                saved.getUsername(),
                saved.getFullName(),
                saved.getBio(),
                saved.getProfilePicUrl());

        return saved;
    }

    @Override
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with email: " + email));

        ensureActive(user);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password.");
        }
        return jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
    }

    @Override
    public String loginWithGoogle(String idToken) {
        Map<String, Object> googleUser = verifyGoogleToken(idToken);
        String email = stringValue(googleUser.get("email"));

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(buildUniqueUsername(email, stringValue(googleUser.get("name"))));
            newUser.setFullName(stringValue(googleUser.get("name")));
            newUser.setProfilePicUrl(stringValue(googleUser.get("picture")));
            newUser.setProvider("GOOGLE");
            newUser.setRole("USER");
            newUser.setActive(true);
            return userRepository.save(newUser);
        });

        ensureActive(user);

        boolean changed = false;
        if (user.getProvider() == null || user.getProvider().isBlank() || "LOCAL".equals(user.getProvider())) {
            user.setProvider("GOOGLE");
            changed = true;
        }
        if ((user.getFullName() == null || user.getFullName().isBlank())
                && !stringValue(googleUser.get("name")).isBlank()) {
            user.setFullName(stringValue(googleUser.get("name")));
            changed = true;
        }
        if ((user.getProfilePicUrl() == null || user.getProfilePicUrl().isBlank())
                && !stringValue(googleUser.get("picture")).isBlank()) {
            user.setProfilePicUrl(stringValue(googleUser.get("picture")));
            changed = true;
        }

        if (changed) {
            user = userRepository.save(user);
        }

        searchClient.indexUser(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getBio(),
                user.getProfilePicUrl());

        return jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
    }

    @Override
    public void logout(String token) {
        // Stateless JWT — client discards token.
        // TODO: add Redis blacklist for production.
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token. Please log in again.");
        }
        int userId = jwtUtil.getUserIdFromToken(token);
        User user = getUserById(userId);
        return jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public User getUserById(int userId) {
        // Use findById (standard JPA primary-key lookup) not findByUserId
        // (which caused a wrong SQL query in older versions)
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }



    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);

        if (updatedUser.getFullName() != null) existing.setFullName(updatedUser.getFullName());
        if (updatedUser.getBio() != null) existing.setBio(updatedUser.getBio());
        if (updatedUser.getProfilePicUrl()!= null) existing.setProfilePicUrl(updatedUser.getProfilePicUrl());
        if (updatedUser.getUsername() != null
                && !updatedUser.getUsername().equals(existing.getUsername())) {
            if (userRepository.existsByUsername(updatedUser.getUsername())) {
                throw new RuntimeException("Username already taken: " + updatedUser.getUsername());
            }
            existing.setUsername(updatedUser.getUsername());
        }

        User saved = userRepository.save(existing);
        searchClient.indexUser(
                saved.getUserId(),
                saved.getUsername(),
                saved.getFullName(),
                saved.getBio(),
                saved.getProfilePicUrl());

        return saved;
    }

    @Override
    public void changePassword(int userId, String newPassword) {
        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void deactivateAccount(int userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
        searchClient.removeUserIndex(userId);
        DeactivationEvent event = new DeactivationEvent(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                LocalDateTime.now()
        );
        deactivationProducer.sendDeactivationEvent(event);
    }


    @Override
    public List<User> searchUsers(String query) {
        return userRepository.searchByUsername(query);
    }


    @Override
    public int getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }

    @Override
    public String getUsernameFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }

    // ─── ADMIN: GET ALL USERS ─────────────────────────────────────────────────

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ─── ADMIN: GET DEACTIVATED USERS ─────────────────────────────────────────

    @Override
    public List<User> getDeactivatedUsers() {
        return userRepository.findAllByIsActive(false);
    }

    // ─── ADMIN: REACTIVATE ACCOUNT ────────────────────────────────────────────
    // Called by admin when they approve a reactivation request.
    // Re-indexes the user in Elasticsearch so they appear in search again.

    @Override
    public User reactivateAccount(int userId) {
        User user = getUserById(userId);
        user.setActive(true);
        User saved = userRepository.save(user);

        // Re-index in Elasticsearch
        searchClient.indexUser(saved.getUserId(), saved.getUsername(),
                saved.getFullName(), saved.getBio(), saved.getProfilePicUrl());

        DeactivationEvent event = new DeactivationEvent(
                saved.getUserId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFullName(),
                LocalDateTime.now()
        );
        deactivationProducer.sendReactivationApproved(event);

        System.out.println("[auth-service] Account reactivated: " + saved.getUsername());
        return saved;
    }

    // ─── ADMIN: HARD DELETE USER ──────────────────────────────────────────────
    // Permanently removes the user row. Use with caution.

    @Transactional
    @Override
    public void hardDeleteUser(int userId) {
        User user = getUserById(userId);
        searchClient.removeUserIndex(userId);
        userRepository.deleteByUserId(userId);
        System.out.println("[auth-service] User hard-deleted: " + user.getUsername());
    }

    // ─── ADMIN: CHANGE ROLE ───────────────────────────────────────────────────

    @Override
    public User changeUserRole(int userId, String newRole) {
        if (!newRole.equals("USER") && !newRole.equals("ADMIN")) {
            throw new RuntimeException("Invalid role. Must be USER or ADMIN.");
        }
        User user = getUserById(userId);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    private void ensureActive(User user) {
        if (user.isActive()) {
            return;
        }

        DeactivationEvent reactivationRequest = new DeactivationEvent(
                user.getUserId(), user.getUsername(), user.getEmail(),
                user.getFullName(), LocalDateTime.now()
        );
        deactivationProducer.sendReactivationRequest(reactivationRequest);
        throw new RuntimeException(
                "Account is deactivated. A reactivation request has been sent to the admin. " +
                        "You will be notified at " + user.getEmail() + " when your account is restored."
        );
    }

    private Map<String, Object> verifyGoogleToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new RuntimeException("Google credential is required.");
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new RuntimeException("Google OAuth client ID is not configured.");
        }

        String tokenInfoUrl = UriComponentsBuilder
                .fromUriString("https://oauth2.googleapis.com/tokeninfo")
                .queryParam("id_token", idToken)
                .toUriString();

        try {
            ResponseEntity<Map> response = new RestTemplate().getForEntity(tokenInfoUrl, Map.class);
            Map<String, Object> payload = response.getBody();
            if (payload == null) {
                throw new RuntimeException("Google token verification failed.");
            }

            String audience = stringValue(payload.get("aud"));
            String email = stringValue(payload.get("email"));
            boolean emailVerified = Boolean.parseBoolean(stringValue(payload.get("email_verified")));

            if (!googleClientId.equals(audience)) {
                throw new RuntimeException("Google credential audience does not match this application.");
            }
            if (email.isBlank() || !emailVerified) {
                throw new RuntimeException("Google account email must be verified.");
            }

            return payload;
        } catch (RuntimeException ex) {
            throw new RuntimeException("Invalid Google credential.");
        }
    }

    private String buildUniqueUsername(String email, String name) {
        String source = !name.isBlank() ? name : email.substring(0, email.indexOf("@"));
        String base = source.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        if (base.length() < 3) {
            base = "user_" + base;
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffixText = "_" + suffix++;
            int maxBaseLength = Math.min(base.length(), 50 - suffixText.length());
            candidate = base.substring(0, maxBaseLength) + suffixText;
        }
        return candidate;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
