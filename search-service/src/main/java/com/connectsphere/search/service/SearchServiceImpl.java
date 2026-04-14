package com.connectsphere.search.service;

import com.connectsphere.search.entity.*;
import com.connectsphere.search.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private HashtagRepository hashtagRepository;

    @Autowired
    private PostHashtagRepository postHashtagRepository;

    @Autowired
    private PostSearchRepository postSearchRepository;

    @Autowired
    private UserSearchRepository userSearchRepository;

    // Regex: matches #tag tokens in post content
    // Captures word characters after # that are NOT preceded by another word char
    private static final Pattern HASHTAG_PATTERN =
            Pattern.compile("(?<![\\w])#([\\w]+)");

    // ─── INDEX POST ───────────────────────────────────────────────────────────
    // Two things happen when a post is indexed:
    //   1. PostDocument is saved to Elasticsearch (for full-text search)
    //   2. #tags are extracted, upserted in MySQL Hashtag table,
    //      and PostHashtag mappings are created

    @Override
    @Transactional
    public void indexPost(int postId, String content, int authorId,
                          String visibility, String postType) {

        // ── Step 1: Extract hashtags from content ──────────────────────────
        List<String> tags = extractHashtags(content);

        // ── Step 2: Upsert each hashtag in MySQL ───────────────────────────
        for (String tag : tags) {
            Optional<Hashtag> existing = hashtagRepository.findByTag(tag);
            if (existing.isPresent()) {
                // Tag already exists — increment its post count atomically
                hashtagRepository.incrementPostCount(tag);
            } else {
                // New tag — create it with count = 1
                Hashtag newTag = new Hashtag();
                newTag.setTag(tag);
                newTag.setPostCount(1);
                hashtagRepository.save(newTag);
            }

            // ── Step 3: Create PostHashtag mapping if not already present ──
            Hashtag hashtag = hashtagRepository.findByTag(tag).get();
            if (!postHashtagRepository.existsByPostIdAndHashtagId(postId, hashtag.getHashtagId())) {
                PostHashtag ph = new PostHashtag();
                ph.setPostId(postId);
                ph.setHashtagId(hashtag.getHashtagId());
                postHashtagRepository.save(ph);
            }
        }

        // ── Step 4: Save to Elasticsearch (only PUBLIC posts are indexed) ──
        // FOLLOWERS_ONLY and PRIVATE posts are excluded from search results
        if ("PUBLIC".equals(visibility)) {
            PostDocument doc = new PostDocument();
            doc.setId(String.valueOf(postId));
            doc.setPostId(postId);
            doc.setAuthorId(authorId);
            doc.setContent(content);
            doc.setVisibility(visibility);
            doc.setPostType(postType);
            doc.setHashtags(tags);
            postSearchRepository.save(doc);
        }

        System.out.println("[search-service] Indexed post " + postId
                + " with " + tags.size() + " hashtags: " + tags);
    }

    // ─── REMOVE POST INDEX ────────────────────────────────────────────────────
    // Removes post from Elasticsearch and decrements hashtag counts in MySQL.

    @Override
    @Transactional
    public void removePostIndex(int postId) {
        // Get existing PostHashtag mappings to know which tags to decrement
        List<PostHashtag> mappings = postHashtagRepository.findByPostId(postId);

        for (PostHashtag ph : mappings) {
            hashtagRepository.findById(ph.getHashtagId())
                    .ifPresent(hashtag -> hashtagRepository.decrementPostCount(hashtag.getTag()));
        }

        // Delete PostHashtag mappings
        postHashtagRepository.deleteByPostId(postId);

        // Delete from Elasticsearch
        postSearchRepository.deleteById(String.valueOf(postId));

        System.out.println("[search-service] Removed index for post " + postId);
    }

    // ─── SEARCH POSTS ─────────────────────────────────────────────────────────
    // Full-text search via Elasticsearch — returns postIds.
    // The caller (SearchResource) returns these IDs to the client,
    // which then fetches full post data from post-service.

    @Override
    public List<Integer> searchPosts(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        List<PostDocument> results = postSearchRepository.searchByContent(keyword);
        return results.stream()
                .map(PostDocument::getPostId)
                .collect(Collectors.toList());
    }

    // ─── SEARCH USERS ─────────────────────────────────────────────────────────
    // Full-text search via Elasticsearch — returns userIds.

    @Override
    public List<Integer> searchUsers(String query) {
        if (query == null || query.isBlank()) return List.of();
        List<UserDocument> results = userSearchRepository.searchByUsernameOrFullName(query);
        return results.stream()
                .map(UserDocument::getUserId)
                .collect(Collectors.toList());
    }

    // ─── GET HASHTAGS FOR POST ────────────────────────────────────────────────

    @Override
    public List<Hashtag> getHashtagsForPost(int postId) {
        List<PostHashtag> mappings = postHashtagRepository.findByPostId(postId);
        List<Hashtag> hashtags = new ArrayList<>();
        for (PostHashtag ph : mappings) {
            hashtagRepository.findById(ph.getHashtagId()).ifPresent(hashtags::add);
        }
        return hashtags;
    }

    // ─── GET TRENDING HASHTAGS ────────────────────────────────────────────────
    // Returns top N hashtags sorted by postCount descending.
    // MySQL query with LIMIT — efficient even with millions of hashtags.

    @Override
    public List<Hashtag> getTrendingHashtags(int limit) {
        return hashtagRepository.findTrendingHashtags(PageRequest.of(0, limit));
    }

    // ─── GET POSTS BY HASHTAG ─────────────────────────────────────────────────
    // Uses Elasticsearch for fast tag-based lookup.

    @Override
    public List<Integer> getPostsByHashtag(String tag) {
        // Normalise — strip leading # if present
        String normalised = tag.startsWith("#") ? tag.substring(1) : tag;
        List<PostDocument> docs = postSearchRepository.findByHashtag(normalised.toLowerCase());
        return docs.stream().map(PostDocument::getPostId).collect(Collectors.toList());
    }

    // ─── SEARCH HASHTAGS ──────────────────────────────────────────────────────
    // Partial match from MySQL — used for #tag autocomplete in the frontend.

    @Override
    public List<Hashtag> searchHashtags(String query) {
        String normalised = query.startsWith("#") ? query.substring(1) : query;
        return hashtagRepository.findByTagContainingIgnoreCase(normalised);
    }

    // ─── GET HASHTAG COUNT ────────────────────────────────────────────────────

    @Override
    public int getHashtagCount(String tag) {
        String normalised = tag.startsWith("#") ? tag.substring(1) : tag;
        return hashtagRepository.findByTag(normalised)
                .map(Hashtag::getPostCount)
                .orElse(0);
    }

    // ─── INDEX USER ───────────────────────────────────────────────────────────
    // Upsert a UserDocument into Elasticsearch.

    @Override
    public void indexUser(int userId, String username, String fullName,
                          String bio, String profilePicUrl) {
        UserDocument doc = new UserDocument();
        doc.setId(String.valueOf(userId));
        doc.setUserId(userId);
        doc.setUsername(username);
        doc.setFullName(fullName);
        doc.setBio(bio);
        doc.setProfilePicUrl(profilePicUrl);
        userSearchRepository.save(doc);
        System.out.println("[search-service] Indexed user " + userId + " (@" + username + ")");
    }

    // ─── REMOVE USER INDEX ────────────────────────────────────────────────────

    @Override
    public void removeUserIndex(int userId) {
        userSearchRepository.deleteById(String.valueOf(userId));
        System.out.println("[search-service] Removed user index for userId=" + userId);
    }

    // ─── HASHTAG PARSER ───────────────────────────────────────────────────────
    // Extracts unique lowercase tag strings (without #) from post content.

    private List<String> extractHashtags(String content) {
        List<String> tags = new ArrayList<>();
        if (content == null || content.isBlank()) return tags;

        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase();
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
