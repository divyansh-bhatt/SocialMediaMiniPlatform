package com.connectsphere.search.service;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostDocument;
import com.connectsphere.search.entity.UserDocument;

import java.util.List;

public interface SearchService {

    // ── Post Indexing ──────────────────────────────────────────────────────────

    /**
     * Index a post into Elasticsearch and extract/upsert its hashtags in MySQL.
     * Called by post-service after createPost() and updatePost().
     *
     * @param postId   the post's ID
     * @param content  the post's text content (searched for #tags)
     * @param authorId the post author's userId
     * @param visibility PUBLIC | FOLLOWERS_ONLY | PRIVATE (only PUBLIC is indexed in ES)
     * @param postType TEXT | MEDIA
     */
    void indexPost(int postId, String content, int authorId, String visibility, String postType);

    /**
     * Remove a post from Elasticsearch and decrement hashtag counts.
     * Called by post-service after deletePost().
     */
    void removePostIndex(int postId);

    // ── Search ────────────────────────────────────────────────────────────────

    /** Full-text search across post content — returns matching postIds */
    List<Integer> searchPosts(String keyword);

    /** Full-text search across username and fullName — returns matching userIds */
    List<Integer> searchUsers(String query);

    // ── Hashtag Operations ────────────────────────────────────────────────────

    /** Get all hashtags attached to a specific post */
    List<Hashtag> getHashtagsForPost(int postId);

    /**
     * Get top N trending hashtags ranked by postCount descending.
     * Default limit = 20.
     */
    List<Hashtag> getTrendingHashtags(int limit);

    /** Get all postIds that use a given hashtag string (without #) */
    List<Integer> getPostsByHashtag(String tag);

    /** Partial-match hashtag search — for autocomplete */
    List<Hashtag> searchHashtags(String query);

    /** Get post count for a specific hashtag */
    int getHashtagCount(String tag);

    // ── User Indexing ─────────────────────────────────────────────────────────

    /**
     * Index or update a user document in Elasticsearch.
     * Called by auth-service after register() and updateProfile().
     */
    void indexUser(int userId, String username, String fullName,
                   String bio, String profilePicUrl);

    /** Remove a user from the Elasticsearch index (on account deactivation) */
    void removeUserIndex(int userId);
}
