package com.connectsphere.auth.service;

import com.connectsphere.auth.client.SearchClient;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
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

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Please contact support.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password.");
        }
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
}
