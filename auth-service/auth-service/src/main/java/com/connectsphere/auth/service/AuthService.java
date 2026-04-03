package com.connectsphere.auth.service;

import com.connectsphere.auth.entity.User;

import java.util.List;
import java.util.Map;

public interface AuthService {

    // Register a new user (email/password)
    User register(User user);

    // Login — returns JWT token string
    String login(String email, String password);

    // Logout (stateless JWT — client discards token; server can blacklist if needed)
    void logout(String token);

    // Validate a JWT token — returns true if valid
    boolean validateToken(String token);

    // Refresh an expiring token
    String refreshToken(String token);

    // Get user by email
    User getUserByEmail(String email);

    // Get user by ID
    User getUserById(int userId);

    // Update profile fields (fullName, bio, profilePicUrl, username)
    User updateProfile(int userId, User updatedUser);

    // Change password
    void changePassword(int userId, String newPassword);

    // Deactivate account (soft delete — sets isActive = false)
    void deactivateAccount(int userId);

    // Search users by username or full name
    List<User> searchUsers(String query);

    // Extract userId from JWT token
    int getUserIdFromToken(String token);

    // Extract username from JWT token
    String getUsernameFromToken(String token);
}
