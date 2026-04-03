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

    // ─── POST /auth/register ──────────────────────────────────────────────────
    // Body: { "username": "john", "email": "john@email.com", "passwordHash": "secret123" }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        User registered = authService.register(user);
        // Don't return the password hash in the response
        registered.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    // ─── POST /auth/login ─────────────────────────────────────────────────────
    // Body: { "email": "john@email.com", "password": "secret123" }
    // Returns: { "token": "eyJhbGci..." }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        String token = authService.login(email, password);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // ─── POST /auth/logout ────────────────────────────────────────────────────
    // Header: Authorization: Bearer <token>

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // ─── POST /auth/refresh ───────────────────────────────────────────────────
    // Body: { "token": "eyJhbGci..." }
    // Returns: { "token": "eyJhbGci..." }  (new token)

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String newToken = authService.refreshToken(body.get("token"));
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    // ─── GET /auth/validate ───────────────────────────────────────────────────
    // Query param: ?token=eyJhbGci...
    // Used by other microservices to validate tokens

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validate(@RequestParam String token) {
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // ─── GET /auth/profile ────────────────────────────────────────────────────
    // Header: Authorization: Bearer <token>

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        User user = authService.getUserById(userId);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    // ─── GET /auth/profile/{userId} ───────────────────────────────────────────
    // Public: get any user's profile by ID (for other services to call)

    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfileById(@PathVariable int userId) {
        User user = authService.getUserById(userId);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    // ─── PUT /auth/profile ────────────────────────────────────────────────────
    // Header: Authorization: Bearer <token>
    // Body: { "fullName": "John Doe", "bio": "Developer", "profilePicUrl": "..." }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@RequestBody User updatedUser,
                                               HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        User updated = authService.updateProfile(userId, updatedUser);
        updated.setPasswordHash(null);
        return ResponseEntity.ok(updated);
    }

    // ─── PUT /auth/password ───────────────────────────────────────────────────
    // Header: Authorization: Bearer <token>
    // Body: { "newPassword": "newSecret123" }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body,
                                                               HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        authService.changePassword(userId, body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    // ─── GET /auth/search ─────────────────────────────────────────────────────
    // Query param: ?query=john
    // Public: search users by username or full name

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        List<User> users = authService.searchUsers(query);
        // Remove password hashes from results
        users.forEach(u -> u.setPasswordHash(null));
        return ResponseEntity.ok(users);
    }

    // ─── DELETE /auth/deactivate ──────────────────────────────────────────────
    // Header: Authorization: Bearer <token>

    @DeleteMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivate(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully."));
    }

    // ─── GLOBAL EXCEPTION HANDLER ─────────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
