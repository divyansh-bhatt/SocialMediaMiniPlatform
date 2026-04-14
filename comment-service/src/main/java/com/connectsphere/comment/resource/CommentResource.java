package com.connectsphere.comment.resource;

import com.connectsphere.comment.dto.CommentDTO;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments")
public class CommentResource {

    @Autowired
    private CommentService commentService;

    // Add a top-level comment OR a reply.
    // For a top-level comment: { "postId": 1, "content": "Great post!" }
    // For a reply:             { "postId": 1, "parentCommentId": 5, "content": "I agree!" }
    // authorId is always taken from JWT — never from the request body

    @PostMapping
    public ResponseEntity<Comment> addComment(@RequestBody Comment comment,
                                              HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        comment.setAuthorId(userId); // always set from JWT

        Comment saved = commentService.addComment(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // All comments (top-level + replies) for a post — public

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getByPost(@PathVariable int postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }
    // Only top-level comments — replies fetched separately per comment
    // This is the recommended approach for rendering threaded UI

    @GetMapping("/post/{postId}/top-level")
    public ResponseEntity<List<Comment>> getTopLevel(@PathVariable int postId) {
        return ResponseEntity.ok(commentService.getTopLevelComments(postId));
    }

    // Get a single comment by ID — public

    @GetMapping("/{commentId}")
    public ResponseEntity<Comment> getCommentById(@PathVariable int commentId) {
        return commentService.getCommentById(commentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get all replies to a comment — public

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable int commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    // Update comment content — author only
    // Body: { "content": "Updated text" }

    @PutMapping("/{commentId}")
    public ResponseEntity<Comment> updateComment(@PathVariable int commentId,
                                                  @RequestBody Map<String, String> body,
                                                  HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");

        Comment existing = commentService.getCommentById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        // Ownership check — only the author can edit
        if (existing.getAuthorId() != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Comment updated = commentService.updateComment(commentId, body.get("content"));
        return ResponseEntity.ok(updated);
    }

    // Soft-delete — author or admin only

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(@PathVariable int commentId,
                                                              HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");

        Comment existing = commentService.getCommentById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        // Allow author OR admin to delete
        String role = (String) request.getAttribute("role");
        if (existing.getAuthorId() != userId && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        commentService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of("message", "Comment deleted."));
    }

    // Get all comments by a user — for profile view

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Comment>> getByUser(@PathVariable int userId) {
        return ResponseEntity.ok(commentService.getCommentsByUser(userId));
    }
    // Internal: called by like-service to resolve comment owner for LIKE notifications.
    @GetMapping("/internal/{commentId}/author-id")
    public ResponseEntity<Integer> getCommentAuthorId(@PathVariable int commentId) {
        return commentService.getCommentById(commentId)
                .map(c -> ResponseEntity.ok(c.getAuthorId()))
                .orElse(ResponseEntity.notFound().build());
    }
    // Like a comment — JWT required

    @PostMapping("/internal/{commentId}/like/increment")
    public ResponseEntity<Void> incrementComment(@PathVariable int commentId) {
        commentService.likeComment(commentId);
        return ResponseEntity.ok().build();
    }

    // Unlike a comment — JWT required

    @PostMapping("/internal/{commentId}/likes/decrement")
    public ResponseEntity<Void> decrementComment(@PathVariable int commentId) {
        commentService.unlikeComment(commentId);
        return ResponseEntity.ok().build();
    }

    // Get total comment count for a post — public

    @GetMapping("/count/{postId}")
    public ResponseEntity<Integer> getCommentCount(@PathVariable int postId) {
        return ResponseEntity.ok(commentService.getCommentCount(postId));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @GetMapping("/post/{postId}/threaded")
    public ResponseEntity<List<CommentDTO>> getThreaded(@PathVariable int postId) {
        return ResponseEntity.ok(commentService.getThreadedComments(postId));
    }
}