package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Integer> {

    /** All media uploaded by a user */
    List<Media> findByUploaderIdAndIsDeletedFalse(int uploaderId);

    /** Find by mediaId (active only) */
    Optional<Media> findByMediaIdAndIsDeletedFalse(int mediaId);

    /** All media linked to a specific post */
    List<Media> findByLinkedPostIdAndIsDeletedFalse(int linkedPostId);

    /** All media of a given type for a user (IMAGE or VIDEO) */
    List<Media> findByUploaderIdAndMediaTypeAndIsDeletedFalse(int uploaderId, String mediaType);

    /** Soft-delete — called when the linked post is deleted */
    void deleteByMediaId(int mediaId);
}
