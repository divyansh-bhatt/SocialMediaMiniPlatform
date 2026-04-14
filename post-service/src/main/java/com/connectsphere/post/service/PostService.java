package com.connectsphere.post.service;

import com.connectsphere.post.entity.Post;

import java.util.List;
import java.util.Optional;

public interface PostService {

    Post createPost(Post post);
    Optional<Post> getPostById(int postId);
    List<Post> getPostsByUser(int authorId);
    List<Post> getFeedForUser(int userId);
    Post updatePost(int postId, Post updatedPost);
    void deletePost(int postId);
    List<Post> searchPosts(String keyword);
    void incrementLikes(int postId);
    void decrementLikes(int postId);
    void incrementComments(int postId);
    void decrementComments(int postId);
    void changeVisibility(int postId, String visibility);
    int getPostCount(int authorId);
}
