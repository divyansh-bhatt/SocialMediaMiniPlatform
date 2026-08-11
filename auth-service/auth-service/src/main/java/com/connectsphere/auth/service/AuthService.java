package com.connectsphere.auth.service;

import com.connectsphere.auth.entity.User;

import java.util.List;
import java.util.Map;

public interface AuthService {
    User register(User user);
    String login(String email, String password);
    String loginWithGoogle(String idToken);
    void logout(String token);
    boolean validateToken(String token);
    String refreshToken(String token);
    User getUserByEmail(String email);
    User getUserById(int userId);
    java.util.Optional<User> getUserByUsername(String username);
    User updateProfile(int userId, User updatedUser);
    void changePassword(int userId, String newPassword);
    void deactivateAccount(int userId);
    List<User> searchUsers(String query);
    int getUserIdFromToken(String token);
    String getUsernameFromToken(String token);
    // Get all users in the system (admin only)
    List<User> getAllUsers();

    // Get all deactivated users pending reactivation (admin only)
    List<User> getDeactivatedUsers();

    // Reactivate a deactivated account (admin only)
    User reactivateAccount(int userId);
    // Permanently delete a user account and all their data (admin only)
    void hardDeleteUser(int userId);

    // Change a user's role (admin only) — e.g. promote to ADMIN
    User changeUserRole(int userId, String newRole);


}
