package com.connectsphere.follow.repository;

import com.connectsphere.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Integer> {

    // Find a specific follow relationship
    Optional<Follow> findByFollowerIdAndFolloweeId(int followerId, int followeeId);

    // Everyone who follows a user (their followers)
    List<Follow> findByFolloweeId(int followeeId);

    // Everyone a user is following (their followees)
    List<Follow> findByFollowerId(int followerId);

    // Check if relationship exists — used for isFollowing check
    boolean existsByFollowerIdAndFolloweeId(int followerId, int followeeId);

    // How many people follow this user
    int countByFolloweeId(int followeeId);

    // How many people this user follows
    int countByFollowerId(int followerId);

    // Mutual follows: people who BOTH follow each other
    // Used for: mutual friends display, suggested users, trust scoring
    // Returns list of userIds who are mutual connections between user1 and user2
    @Query("SELECT f1.followeeId FROM Follow f1 " +
           "WHERE f1.followerId = :userId1 AND f1.status = 'ACTIVE' " +
           "AND f1.followeeId IN (" +
           "  SELECT f2.followerId FROM Follow f2 " +
           "  WHERE f2.followeeId = :userId1 AND f2.status = 'ACTIVE'" +
           ")")
    List<Integer> findMutualFollows(@Param("userId1") int userId1);

    // Delete a follow relationship (for unfollow)
    void deleteByFollowerIdAndFolloweeId(int followerId, int followeeId);

    // Get just the IDs of people a user follows — used by post-service for feed
    // Returns List<Integer> of followeeIds
    @Query("SELECT f.followeeId FROM Follow f WHERE f.followerId = :followerId AND f.status = 'ACTIVE'")
    List<Integer> findFolloweeIdsByFollowerId(@Param("followerId") int followerId);

    // Active follows only
    List<Follow> findByFollowerIdAndStatus(int followerId, String status);
    List<Follow> findByFolloweeIdAndStatus(int followeeId, String status);
}
