package com.connectsphere.follow.service;

import com.connectsphere.follow.client.NotificationClient;
import com.connectsphere.follow.client.UserLookupClient;
import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.repository.FollowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private UserLookupClient userLookupClient;

    @Override
    @Transactional
    public Follow follow(int followerId, int followeeId) {
        // Cannot follow yourself
        if (followerId == followeeId) {
            throw new RuntimeException("You cannot follow yourself.");
        }

        // Check already following
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new RuntimeException("You are already following this user.");
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        follow.setStatus("ACTIVE");
        Follow saved;
        try {
            saved = followRepository.save(follow);
        } catch (DataIntegrityViolationException e) {
            // Race condition safety — DB unique constraint catches concurrent follows
            throw new RuntimeException("Already following this user.");
        }
        String followerUsername = userLookupClient.getUsernameById(followerId);
        notificationClient.sendFollowNotification(followeeId, followerId, followerUsername);
        return saved;
    }

    @Override
    @Transactional
    public void unfollow(int followerId, int followeeId) {
        if (!followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new RuntimeException("You are not following this user.");
        }
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public boolean isFollowing(int followerId, int followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    @Override
    public List<Follow> getFollowing(int followerId) {
        return followRepository.findByFollowerIdAndStatus(followerId, "ACTIVE");
    }

    @Override
    public List<Follow> getFollowers(int followeeId) {
        return followRepository.findByFolloweeIdAndStatus(followeeId, "ACTIVE");
    }

    @Override
    public int getFollowerCount(int followeeId) {
        return followRepository.countByFolloweeId(followeeId);
    }

    @Override
    public int getFollowingCount(int followerId) {
        return followRepository.countByFollowerId(followerId);
    }

    // Returns userIds that BOTH users follow each other
    // Used for: "X mutual friends" display on profiles
    @Override
    public List<Integer> getMutualFollows(int userId1, int userId2) {
        // Get everyone userId1 follows
        Set<Integer> user1Following = followRepository
            .findFolloweeIdsByFollowerId(userId1)
            .stream()
            .collect(Collectors.toSet());

        // Get everyone userId2 follows
        Set<Integer> user2Following = followRepository
            .findFolloweeIdsByFollowerId(userId2)
            .stream()
            .collect(Collectors.toSet());

        // Intersection = mutual connections
        user1Following.retainAll(user2Following);
        return new ArrayList<>(user1Following);
    }
    // Second-degree connections: "People followed by people you follow"
    // Algorithm:
    //   1. Get all users you follow (first-degree)
    //   2. Get all users THEY follow (second-degree)
    //   3. Remove: yourself, people you already follow
    //   4. Rank by frequency (most suggested first)
    //   5. Return top 10

    @Override
    public List<Integer> getSuggestedUsers(int userId) {
        // Step 1: your followees
        List<Integer> myFollowees = followRepository.findFolloweeIdsByFollowerId(userId);

        if (myFollowees.isEmpty()) {
            // No connections yet — return empty (could return popular users later)
            return List.of();
        }

        // Step 2: people your followees follow (second-degree)
        Map<Integer, Integer> suggestionScore = new HashMap<>();
        for (int followeeId : myFollowees) {
            List<Integer> theirFollowees = followRepository
                .findFolloweeIdsByFollowerId(followeeId);
            for (int candidate : theirFollowees) {
                // Skip yourself and people you already follow
                if (candidate != userId && !myFollowees.contains(candidate)) {
                    // Each time someone you follow also follows this person, score goes up
                    suggestionScore.merge(candidate, 1, Integer::sum);
                }
            }
        }

        // Step 3: sort by score descending, return top 10
        return suggestionScore.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    // Called by post-service via GET /follows/{userId}/following-ids
    // Returns plain List<Integer> — post-service uses this to build the news feed

    @Override
    public List<Integer> getFollowingIds(int followerId) {
        return followRepository.findFolloweeIdsByFollowerId(followerId);
    }
}
