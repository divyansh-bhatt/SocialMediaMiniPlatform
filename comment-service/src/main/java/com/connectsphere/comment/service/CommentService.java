package com.connectsphere.comment.service;

import com.connectsphere.comment.dto.CommentDTO;
import com.connectsphere.comment.entity.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentService {
    Comment addComment(Comment comment);
    List<Comment> getCommentsByPost(int postId);
    List<Comment> getTopLevelComments(int postId);
    Optional<Comment> getCommentById(int commentId);
    List<Comment> getReplies(int parentCommentId);
    Comment updateComment(int commentId, String newContent);
    void deleteComment(int commentId);
    List<Comment> getCommentsByUser(int authorId);
    void likeComment(int commentId);
    void unlikeComment(int commentId);
    int getCommentCount(int postId);

    List<CommentDTO> getThreadedComments(int postId);
}
