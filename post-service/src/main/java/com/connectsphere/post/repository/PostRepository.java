package com.connectsphere.post.repository;

import com.connectsphere.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    // All posts by a specific author (excluding deleted)
    List<Post> findByAuthorIdAndIsDeletedFalse(int authorId);

    // Find post by ID (excluding deleted)
    Optional<Post> findByPostIdAndIsDeletedFalse(int postId);

    // Find public posts only (for guest browsing)
    List<Post> findByVisibilityAndIsDeletedFalse(String visibility);

    // News feed: get posts from a list of followee IDs, newest first
    // Called by post-service after follow-service returns followee IDs
    @Query("SELECT p FROM Post p WHERE p.authorId IN :userIds " +
           "AND p.isDeleted = false " +
           "AND p.visibility IN ('PUBLIC', 'FOLLOWERS_ONLY') " +
           "ORDER BY p.createdAt DESC")
    List<Post> findFeedByUserIds(@Param("userIds") List<Integer> userIds);

    // Full-text search on post content
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:keyword% " +
           "AND p.isDeleted = false AND p.visibility = 'PUBLIC'")
    List<Post> searchByContent(@Param("keyword") String keyword);

    // All posts by author ordered by newest first (for profile view)
    List<Post> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(int authorId);

    // Count total posts by author
    int countByAuthorIdAndIsDeletedFalse(int authorId);
}
