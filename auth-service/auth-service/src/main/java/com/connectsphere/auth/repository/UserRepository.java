package com.connectsphere.auth.repository;

import com.connectsphere.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Find by email (used for login)
    Optional<User> findByEmail(String email);

    // Find by username
    Optional<User> findByUsername(String username);

    // Find by userId
    Optional<User> findByUserId(int userId);

    // Check if email already registered
    boolean existsByEmail(String email);

    // Check if username already taken
    boolean existsByUsername(String username);

    // Get all users by role (e.g., find all ADMINs)
    List<User> findAllByRole(String role);

    // Search users by username (partial match) — for search feature
    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.fullName LIKE %:query%")
    List<User> searchByUsername(@Param("query") String query);

    // Delete by userId
    void deleteByUserId(int userId);
}
