package com.connectsphere.comment.service;

import com.connectsphere.comment.client.NotificationClient;
import com.connectsphere.comment.client.UserLookupClient;
import com.connectsphere.comment.dto.CommentDTO;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.repository.CommentRepository;
import com.connectsphere.comment.util.MentionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private UserLookupClient userLookupClient;
    @Value("${post.service.url}")
    private String postServiceUrl;

    @Override
    public Comment addComment(Comment comment) {
        if (comment.getParentCommentId() != null) {
            Comment parent = commentRepository.findByCommentIdAndIsDeletedFalse(comment.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found: " + comment.getParentCommentId()));
           if (parent.getPostId()!=comment.getPostId()){
               throw new RuntimeException("Parent comment belongs to different post.");
           }
        }
        comment.setDeleted(false);
        comment.setLikesCount(0);
        Comment saved = commentRepository.save(comment);
        notifyPostServiceIncrement(comment.getPostId());
        String actorUsername = userLookupClient.getUsernameById(saved.getAuthorId());

        if (saved.getParentCommentId() == null) {
            // Top-level comment -> notify post author
            int postAuthorId = fetchPostAuthorId(saved.getPostId());
            if (postAuthorId != -1) {
                notificationClient.sendCommentNotification(
                        postAuthorId, saved.getAuthorId(),
                        saved.getPostId(), saved.getCommentId(), actorUsername);
            }
        } else {
            // Reply -> notify parent comment author
            commentRepository.findByCommentIdAndIsDeletedFalse(saved.getParentCommentId())
                    .ifPresent(parent -> notificationClient.sendReplyNotification(
                            parent.getAuthorId(), saved.getAuthorId(),
                            saved.getPostId(), saved.getParentCommentId(),
                            saved.getCommentId(), actorUsername));
        }

        // Parse @mentions in content and notify each mentioned user
        List<String> mentionedUsernames = MentionParser.extractMentions(saved.getContent());
        for (String username : mentionedUsernames) {
            int mentionedUserId = userLookupClient.getUserIdByUsername(username);
            if (mentionedUserId != -1 && mentionedUserId != saved.getAuthorId()) {
                notificationClient.sendMentionNotification(
                        mentionedUserId, saved.getAuthorId(),
                        saved.getPostId(), saved.getCommentId(), actorUsername);
            }
        }
        return saved;
    }

    @Override
    public List<Comment> getCommentsByPost(int postId) {
        return commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
    }

    @Override
    public List<Comment> getTopLevelComments(int postId) {
        return commentRepository.findTopLevelByPostId(postId);
    }

    @Override
    public Optional<Comment> getCommentById(int commentId) {
        return commentRepository.findByCommentIdAndIsDeletedFalse(commentId);
    }

    @Override
    public List<Comment> getReplies(int parentCommentId) {
        return commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentCommentId);
    }

    @Override
    public Comment updateComment(int commentId, String newContent) {
        Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));
        if (newContent == null || newContent.isBlank()) {
            throw new RuntimeException("Comment content cannot be empty.");
        }
        List<String> oldMentions = MentionParser.extractMentions(comment.getContent());
        comment.setContent(newContent);
        Comment updated = commentRepository.save(comment);

        // Only newly added @mentions fire notifications on edit
        List<String> newMentions = new ArrayList<>(MentionParser.extractMentions(newContent));
        newMentions.removeAll(oldMentions);

        if (!newMentions.isEmpty()) {
            String actorUsername = userLookupClient.getUsernameById(updated.getAuthorId());
            for (String username : newMentions) {
                int mentionedUserId = userLookupClient.getUserIdByUsername(username);
                if (mentionedUserId != -1 && mentionedUserId != updated.getAuthorId()) {
                    notificationClient.sendMentionNotification(
                            mentionedUserId, updated.getAuthorId(),
                            updated.getPostId(), updated.getCommentId(), actorUsername);
                }
            }
        }
        return updated;
    }

    @Override
    public void deleteComment(int commentId) {
        Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));
        comment.setDeleted(true);
        commentRepository.save(comment);
        notifyPostServiceDecrement(comment.getPostId());
    }

    @Override
    public List<Comment> getCommentsByUser(int authorId) {
        return commentRepository.findByAuthorIdAndIsDeletedFalse(authorId);
    }

    @Override
    public void likeComment(int commentId) {
        Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));
        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
    }

    @Override
    public void unlikeComment(int commentId) {
        Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));
        comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        commentRepository.save(comment);
    }

    @Override
    public int getCommentCount(int postId) {
        return commentRepository.countByPostIdAndIsDeletedFalse(postId);
    }

    private void notifyPostServiceIncrement(int postId) {
        try {
            restTemplate.postForEntity(postServiceUrl + "/posts/internal/" + postId + "/comments/increment", null, Void.class);
        } catch (Exception e) {
            System.err.println("[comment-service] WARNING: Could not increment commentsCount for postId=" + postId);
        }
    }

    private void notifyPostServiceDecrement(int postId) {
        try {
            restTemplate.postForEntity(postServiceUrl + "/posts/internal/" + postId + "/comments/decrement", null, Void.class);
        } catch (Exception e) {
            System.err.println("[comment-service] WARNING: Could not decrement commentsCount for postId=" + postId);
        }
    }
    public List<CommentDTO> getThreadedComments(int postId) {

        List<Comment> comments =
                commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);

        Map<Integer, List<Comment>> replyMap = new HashMap<>();

        // group replies by parentCommentId
        for (Comment c : comments) {
            if (c.getParentCommentId() != null) {
                replyMap
                        .computeIfAbsent(c.getParentCommentId(), k -> new ArrayList<>())
                        .add(c);
            }
        }

        // build tree from top-level
        List<CommentDTO> result = new ArrayList<>();

        for (Comment c : comments) {
            if (c.getParentCommentId() == null) {
                result.add(buildTree(c, replyMap));
            }
        }

        return result;
    }
    private CommentDTO buildTree(Comment comment,
                                  Map<Integer, List<Comment>> replyMap) {

        CommentDTO node = mapToNode(comment);

        List<Comment> replies = replyMap.get(comment.getCommentId());

        if (replies != null) {
            for (Comment reply : replies) {
                node.getReplies().add(buildTree(reply, replyMap)); // 🔁 recursion
            }
        }
        return node;
    }
    private CommentDTO mapToNode(Comment c) {
        CommentDTO node = new CommentDTO();
        node.setCommentId(c.getCommentId());
        node.setPostId(c.getPostId());
        node.setAuthorId(c.getAuthorId());
        node.setContent(c.getContent());
        node.setLikesCount(c.getLikesCount());
        node.setCreatedAt(c.getCreatedAt());
        return node;
    }

    private int fetchPostAuthorId(int postId) {
        try {
            String url = postServiceUrl + "/posts/internal/" + postId + "/author-id";
            Integer authorId = restTemplate.getForObject(url, Integer.class);
            return authorId != null ? authorId : -1;
        } catch (Exception e) {
            System.err.println("[comment-service] WARNING: Could not fetch authorId for postId=" + postId);
            e.printStackTrace();
            return -1;
        }
    }
}
