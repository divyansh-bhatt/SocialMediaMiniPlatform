package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Integer> {

    /** All active stories by a specific author */
    List<Story> findByAuthorIdAndIsActiveTrue(int authorId);

    /** Active stories for a list of user IDs — used to build the stories feed */
    List<Story> findByAuthorIdInAndIsActiveTrueOrderByCreatedAtDesc(List<Integer> authorIds);

    /** All stories that have expired but are still marked active
     *  — fetched by the scheduler every 5 minutes */
    List<Story> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime now);

    /** Bulk mark expired stories as inactive — more efficient than one-by-one */
    @Modifying
    @Transactional
    @Query("UPDATE Story s SET s.isActive = false WHERE s.isActive = true AND s.expiresAt < :now")
    int markExpiredStories(@Param("now") LocalDateTime now);
}
