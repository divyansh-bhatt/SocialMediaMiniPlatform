package com.connectsphere.search.repository;

import com.connectsphere.search.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Integer> {

    /** Find a hashtag by exact tag text (without #) */
    Optional<Hashtag> findByTag(String tag);

    /**
     * Top N trending hashtags ranked by postCount descending.
     * Called by getTrendingHashtags().
     */
    @Query("SELECT h FROM Hashtag h WHERE h.postCount > 0 ORDER BY h.postCount DESC")
    List<Hashtag> findTrendingHashtags(org.springframework.data.domain.Pageable pageable);

    /** Partial match on tag text — for hashtag autocomplete search */
    List<Hashtag> findByTagContainingIgnoreCase(String query);

    /**
     * Atomic increment of postCount.
     * Used instead of read-modify-write to avoid race conditions.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Hashtag h SET h.postCount = h.postCount + 1, h.lastUsedAt = CURRENT_TIMESTAMP WHERE h.tag = :tag")
    int incrementPostCount(@Param("tag") String tag);

    /**
     * Atomic decrement — never below 0.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Hashtag h SET h.postCount = CASE WHEN h.postCount > 0 THEN h.postCount - 1 ELSE 0 END WHERE h.tag = :tag")
    int decrementPostCount(@Param("tag") String tag);
}
