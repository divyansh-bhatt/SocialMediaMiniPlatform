package com.connectsphere.media.service;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface MediaService {

    Media uploadMedia(MultipartFile file, int uploaderId, Integer linkedPostId) throws IOException;

    List<Media> uploadMultiple(List<MultipartFile> files, int uploaderId, int linkedPostId) throws IOException;

    List<Media> getMediaByPost(int linkedPostId);

    Optional<Media> getMediaById(int mediaId);

    void deleteMedia(int mediaId) throws IOException;

    void softDeleteByPost(int linkedPostId);

    Story createStory(MultipartFile file, int authorId, String caption) throws IOException;

    List<Story> getActiveStoriesByUser(int authorId);

    List<Story> getActiveStoriesForFeed(List<Integer> authorIds);

    void viewStory(int storyId, int viewerId);

    void deleteStory(int storyId, int requestingUserId) throws IOException;

    int expireOldStories();
}
