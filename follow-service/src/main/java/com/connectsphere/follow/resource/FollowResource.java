package com.connectsphere.follow.resource;

import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.service.FollowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/follows")
public class FollowResource {

    @Autowired
    private FollowService followService;

    // Follow a user. JWT required — followerId comes from token.
    // You follow the user identified by {followeeId} in the path.

    @PostMapping("/{followeeId}")
    public ResponseEntity<Follow> follow(@PathVariable int followeeId,
                                         HttpServletRequest request) {
        int followerId = (int) request.getAttribute("userId");
        Follow follow = followService.follow(followerId, followeeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(follow);
    }

    // Unfollow a user. JWT required.
    @DeleteMapping("/{followeeId}")
    public ResponseEntity<Map<String, String>> unfollow(@PathVariable int followeeId,
                                                         HttpServletRequest request) {
        int followerId = (int) request.getAttribute("userId");
        followService.unfollow(followerId, followeeId);
        return ResponseEntity.ok(Map.of("message", "Unfollowed successfully."));
    }
    // Check if one user follows another — public
    // Used by frontend to show Follow/Unfollow button state
    @GetMapping("/is-following")
    public ResponseEntity<Map<String, Object>> isFollowing(
            @RequestParam int followerId,
            @RequestParam int followeeId) {
        boolean result = followService.isFollowing(followerId, followeeId);
        return ResponseEntity.ok(Map.of(
            "followerId", followerId,
            "followeeId", followeeId,
            "isFollowing", result
        ));
    }

    // Get all followers of a user — public
    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<Follow>> getFollowers(@PathVariable int userId) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    // Get all users this user follows — public
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<Follow>> getFollowing(@PathVariable int userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
    }

    // Get total follower count — public
    @GetMapping("/{userId}/follower-count")
    public ResponseEntity<Map<String, Object>> getFollowerCount(@PathVariable int userId) {
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "followerCount", followService.getFollowerCount(userId)
        ));
    }

    // Get total following count — public
    @GetMapping("/{userId}/following-count")
    public ResponseEntity<Map<String, Object>> getFollowingCount(@PathVariable int userId) {
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "followingCount", followService.getFollowingCount(userId)
        ));
    }

    // KEY INTERNAL ENDPOINT — called by post-service to build news feed
    // Returns plain List<Integer> of followee IDs
    // No JWT needed — internal service-to-service call from post-service
    // post-service calls: GET :8085/follows/{userId}/following-ids
    @GetMapping("/{userId}/following-ids")
    public ResponseEntity<List<Integer>> getFollowingIds(@PathVariable int userId) {
        return ResponseEntity.ok(followService.getFollowingIds(userId));
    }

    // Get mutual connections between two users — JWT required
    // Returns List<Integer> of user IDs both follow
    @GetMapping("/mutual")
    public ResponseEntity<List<Integer>> getMutualFollows(
            @RequestParam int userId1,
            @RequestParam int userId2) {
        return ResponseEntity.ok(followService.getMutualFollows(userId1, userId2));
    }

    // Get suggested users to follow — JWT required
    // Based on second-degree connections (friends of friends)
    // Returns List<Integer> of suggested userIds
    @GetMapping("/suggested")
    public ResponseEntity<List<Integer>> getSuggestedUsers(HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        return ResponseEntity.ok(followService.getSuggestedUsers(userId));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(Map.of("error", ex.getMessage()));
    }
}
