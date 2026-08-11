package com.connectsphere.auth.resource;

import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User registered = authService.register(user);
        // Don't return the password hash in the response
        registered.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        String token = authService.login(email, password);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<Map<String, String>> googleLogin(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        String token = authService.loginWithGoogle(credential);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String newToken = authService.refreshToken(body.get("token"));
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestParam String token) {
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        User user = authService.getUserById(userId);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfileById(@PathVariable int userId) {
        User user = authService.getUserById(userId);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }


    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@RequestBody User updatedUser,
                                              HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        User updated = authService.updateProfile(userId, updatedUser);
        updated.setPasswordHash(null);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body,
                                                              HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        authService.changePassword(userId, body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        List<User> users = authService.searchUsers(query);
        // Remove password hashes from results
        users.forEach(u -> u.setPasswordHash(null));
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivate(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully."));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getAllUsers(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        List<User> users = authService.getAllUsers();
        users.forEach(u -> u.setPasswordHash(null));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/admin/users/deactivated")
    public ResponseEntity<List<User>> getDeactivatedUsers(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        List<User> users = authService.getDeactivatedUsers();
        users.forEach(u -> u.setPasswordHash(null));
        return ResponseEntity.ok(users);
    }

    @PutMapping("/admin/users/{userId}/reactivate")
    public ResponseEntity<User> reactivateAccount(@PathVariable int userId,
                                                  HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        User user = authService.reactivateAccount(userId);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/admin/users/{userId}/deactivate")
    public ResponseEntity<Map<String, String>> adminDeactivate(@PathVariable int userId,
                                                               HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated."));
    }

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<Map<String, String>> hardDeleteUser(@PathVariable int userId,
                                                              HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        authService.hardDeleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User permanently deleted."));
    }

    @PutMapping("/admin/users/{userId}/role")
    public ResponseEntity<User> changeRole(@PathVariable int userId,
                                           @RequestBody Map<String, String> body,
                                           HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        User updated = authService.changeUserRole(userId, body.get("role"));
        updated.setPasswordHash(null);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/internal/user-by-username/{username}")
    public ResponseEntity<Map<String, Integer>> getUserIdByUsername(@PathVariable String username) {
        return authService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(Map.of("userId", user.getUserId())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/internal/username-by-id/{userId}")
    public ResponseEntity<Map<String, String>> getUsernameById(@PathVariable int userId) {
        try {
            User user = authService.getUserById(userId);
            return ResponseEntity.ok(Map.of("username", user.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
