package com.connectsphere.like.resource;

import com.connectsphere.like.entity.Like;
import com.connectsphere.like.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/likes")
public class LikeResource {

    @Autowired
    private LikeService likeService;

    // React to a post or comment. JWT required.
    // Body: { "targetId": 1, "targetType": "POST", "reactionType": "LOVE" }
    // targetType: POST or COMMENT
    // reactionType: LIKE, LOVE, HAHA, WOW, SAD, ANGRY

    @PostMapping
    public ResponseEntity<Like> likeTarget(@RequestBody Like like,
                                           HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        like.setUserId(userId); // Always from JWT — never trust client-provided userId
        Like saved = likeService.likeTarget(like);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Remove a reaction. JWT required.
    // Query params: ?targetId=1&targetType=POST

    @DeleteMapping
    public ResponseEntity<Map<String, String>> unlikeTarget(
            @RequestParam int targetId,
            @RequestParam String targetType,
            HttpServletRequest request) {
        int userId = (int) request.getAttribute("userId");
        likeService.unlikeTarget(userId, targetId, targetType);
        return ResponseEntity.ok(Map.of("message", "Reaction removed."));
    }

    // Check if a specific user has reacted to a target — public
    // Query params: ?userId=1&targetId=2&targetType=POST
    // Used by frontend to show filled/empty reaction button

    @GetMapping("/has-liked")
    public ResponseEntity<Map<String, Object>> hasLiked(
            @RequestParam int userId,
            @RequestParam int targetId,
            @RequestParam String targetType) {
        boolean liked = likeService.hasLiked(userId, targetId, targetType);
        return ResponseEntity.ok(Map.of("hasLiked", liked, "userId", userId,
                                        "targetId", targetId, "targetType", targetType));
    }
    // Get all reactions on a post or comment — public
    // Query params: ?targetId=1&targetType=POST

    @GetMapping("/target")
    public ResponseEntity<List<Like>> getLikesByTarget(
            @RequestParam int targetId,
            @RequestParam String targetType) {
        return ResponseEntity.ok(likeService.getLikesByTarget(targetId, targetType));
    }

    // Get all reactions by a user

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Like>> getLikesByUser(@PathVariable int userId) {
        return ResponseEntity.ok(likeService.getLikesByUser(userId));
    }

    // Get total reaction count on a target — public
    // Query params: ?targetId=1&targetType=POST

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getLikeCount(
            @RequestParam int targetId,
            @RequestParam String targetType) {
        int count = likeService.getLikeCount(targetId, targetType);
        return ResponseEntity.ok(Map.of("targetId", targetId,
                                        "targetType", targetType,
                                        "count", count));
    }
    // Count of one specific reaction type on a target — public
    // Query params: ?targetId=1&targetType=POST&reactionType=LOVE

    @GetMapping("/count/by-type")
    public ResponseEntity<Map<String, Object>> getLikeCountByType(
            @RequestParam int targetId,
            @RequestParam String targetType,
            @RequestParam String reactionType) {
        int count = likeService.getLikeCountByType(targetId, targetType, reactionType);
        return ResponseEntity.ok(Map.of("reactionType", reactionType, "count", count));
    }

    // Reaction summary map — the emoji reaction bar data — public
    // Query params: ?targetId=1&targetType=POST
    // Returns: { "LIKE": 42, "LOVE": 7, "HAHA": 2 }
    // Only returns types with at least one reaction

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getReactionSummary(
            @RequestParam int targetId,
            @RequestParam String targetType) {
        return ResponseEntity.ok(likeService.getReactionSummary(targetId, targetType));
    }
    // Change an existing reaction (LIKE → LOVE etc). JWT required.
    // Body: { "targetId": 1, "targetType": "POST", "reactionType": "HAHA" }

    @PutMapping("/reaction")
    public ResponseEntity<Like> changeReaction(@RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        int userId    = (int) request.getAttribute("userId");
        int targetId  = Integer.parseInt(body.get("targetId"));
        String targetType    = body.get("targetType");
        String reactionType  = body.get("reactionType");
        Like updated = likeService.changeReaction(userId, targetId, targetType, reactionType);
        return ResponseEntity.ok(updated);
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(Map.of("error", ex.getMessage()));
    }
}
