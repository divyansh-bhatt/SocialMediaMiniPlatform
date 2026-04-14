package com.connectsphere.like.service;

import com.connectsphere.like.entity.Like;
import java.util.List;
import java.util.Map;

public interface LikeService {

    Like likeTarget(Like like);
    void unlikeTarget(int userId, int targetId, String targetType);
    boolean hasLiked(int userId, int targetId, String targetType);
    List<Like> getLikesByTarget(int targetId, String targetType);
    List<Like> getLikesByUser(int userId);
    int getLikeCount(int targetId, String targetType);

    int getLikeCountByType(int targetId, String targetType, String reactionType);


    Map<String, Long> getReactionSummary(int targetId, String targetType);
    Like changeReaction(int userId, int targetId, String targetType, String newReactionType);
}
