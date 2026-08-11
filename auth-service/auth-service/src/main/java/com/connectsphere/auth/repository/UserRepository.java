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
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    Optional<User> findByUserId(int userId);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    List<User> findAllByRole(String role);
    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.fullName LIKE %:query%")
    List<User> searchByUsername(@Param("query") String query);
    void deleteByUserId(int userId);
    List<User> findAllByIsActive(boolean isActive);
}
