package com.connectsphere.like.repository;

import com.connectsphere.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Integer> {

    // Find a specific user's reaction on a specific target
    // Used for: hasLiked check, changeReaction, unlike
    Optional<Like> findByUserIdAndTargetIdAndTargetType(int userId, int targetId, String targetType);

    // All reactions on a target (post or comment)
    List<Like> findByTargetIdAndTargetType(int targetId, String targetType);

    // All reactions by a user (across all posts and comments)
    List<Like> findByUserId(int userId);

    // Check if a user has reacted to a target — used before allowing like/unlike
    boolean existsByUserIdAndTargetIdAndTargetType(int userId, int targetId, String targetType);

    // Total reaction count on a target
    int countByTargetIdAndTargetType(int targetId, String targetType);

    // Count of a specific reaction type on a target
    // e.g. how many LOVEs on post 5
    int countByTargetIdAndTargetTypeAndReactionType(int targetId, String targetType, String reactionType);

    // Delete a specific user's reaction — for unlike
    void deleteByUserIdAndTargetIdAndTargetType(int userId, int targetId, String targetType);

    // Reaction summary: group by reactionType with counts
    // Returns List of Object[] where [0]=reactionType, [1]=count
    @Query("SELECT l.reactionType, COUNT(l) FROM Like l " +
           "WHERE l.targetId = :targetId AND l.targetType = :targetType " +
           "GROUP BY l.reactionType")
    List<Object[]> getReactionSummaryRaw(@Param("targetId") int targetId,
                                          @Param("targetType") String targetType);
}
