package com.connectsphere.auth.service;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // ─── REGISTER ────────────────────────────────────────────────────────────

    @Override
    public User register(User user) {
        // Check for duplicate email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered: " + user.getEmail());
        }
        // Check for duplicate username
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already taken: " + user.getUsername());
        }

        // Hash the plain-text password before saving
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        // Set defaults
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        if (user.getProvider() == null || user.getProvider().isBlank()) {
            user.setProvider("LOCAL");
        }
        user.setActive(true);

        return userRepository.save(user);
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @Override
    public String login(String email, String password) {
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with email: " + email));

        // Check account is active
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Please contact support.");
        }

        // Verify password against stored bcrypt hash
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password.");
        }

        // Generate and return JWT
        return jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────
    // JWT is stateless — actual invalidation happens on the client side.
    // For production, add a token blacklist in Redis here.

    @Override
    public void logout(String token) {
        // TODO: Add token to Redis blacklist for production use
    }

    // ─── VALIDATE TOKEN ───────────────────────────────────────────────────────

    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    // ─── REFRESH TOKEN ────────────────────────────────────────────────────────

    @Override
    public String refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token. Please log in again.");
        }
        int userId = jwtUtil.getUserIdFromToken(token);
        User user = getUserById(userId);
        return jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
    }

    // ─── GET USER ─────────────────────────────────────────────────────────────

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public User getUserById(int userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    // ─── UPDATE PROFILE ───────────────────────────────────────────────────────

    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);

        // Only update fields that were provided (non-null)
        if (updatedUser.getFullName() != null) {
            existing.setFullName(updatedUser.getFullName());
        }
        if (updatedUser.getBio() != null) {
            existing.setBio(updatedUser.getBio());
        }
        if (updatedUser.getProfilePicUrl() != null) {
            existing.setProfilePicUrl(updatedUser.getProfilePicUrl());
        }
        if (updatedUser.getUsername() != null &&
                !updatedUser.getUsername().equals(existing.getUsername())) {
            // Ensure new username is not taken
            if (userRepository.existsByUsername(updatedUser.getUsername())) {
                throw new RuntimeException("Username already taken: " + updatedUser.getUsername());
            }
            existing.setUsername(updatedUser.getUsername());
        }

        return userRepository.save(existing);
    }

    // ─── CHANGE PASSWORD ──────────────────────────────────────────────────────

    @Override
    public void changePassword(int userId, String newPassword) {
        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── DEACTIVATE ACCOUNT ───────────────────────────────────────────────────

    @Override
    public void deactivateAccount(int userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    // ─── SEARCH USERS ─────────────────────────────────────────────────────────

    @Override
    public List<User> searchUsers(String query) {
        return userRepository.searchByUsername(query);
    }

    // ─── TOKEN HELPERS ────────────────────────────────────────────────────────

    @Override
    public int getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }

    @Override
    public String getUsernameFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }
}
