package com.connectsphere.search.repository;

import com.connectsphere.search.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, Integer> {

    /** All hashtag IDs for a given post */
    List<PostHashtag> findByPostId(int postId);

    /** All post IDs that use a given hashtagId */
    @Query("SELECT ph.postId FROM PostHashtag ph WHERE ph.hashtagId = :hashtagId")
    List<Integer> findPostIdsByHashtagId(@Param("hashtagId") int hashtagId);

    /** Delete all PostHashtag entries for a post (on post removal) */
    @Transactional
    void deleteByPostId(int postId);

    /** Check if a post-hashtag mapping already exists */
    boolean existsByPostIdAndHashtagId(int postId, int hashtagId);
}
