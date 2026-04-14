package com.connectsphere.follow.service;

import com.connectsphere.follow.entity.Follow;
import java.util.List;

public interface FollowService {

    Follow follow(int followerId, int followeeId);

    void unfollow(int followerId, int followeeId);
    boolean isFollowing(int followerId, int followeeId);
    List<Follow> getFollowing(int followerId);
    List<Follow> getFollowers(int followeeId);
    int getFollowerCount(int followeeId);
    int getFollowingCount(int followerId);
    List<Integer> getMutualFollows(int userId1, int userId2);

    // Get suggested users to follow based on second-degree connections
    // "People followed by people you follow"
    List<Integer> getSuggestedUsers(int userId);

    // Get just the followee IDs — used by post-service for news feed
    List<Integer> getFollowingIds(int followerId);
}
