package com.connectsphere.comment.repository;

import com.connectsphere.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(int postId);
    List<Comment> findByAuthorIdAndIsDeletedFalse(int authorId);
    Optional<Comment> findByCommentIdAndIsDeletedFalse(int commentId);
    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(int parentCommentId);

    @Query("SELECT c FROM Comment c WHERE c.postId = :postId AND c.parentCommentId IS NULL AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelByPostId(@Param("postId") int postId);

    int countByPostIdAndIsDeletedFalse(int postId);
    Optional<Comment> findByCommentId(int commentId);
}
