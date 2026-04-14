package com.connectsphere.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MediaServiceImpl implements MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private Cloudinary cloudinary;

    // ─── UPLOAD MEDIA ─────────────────────────────────────────────────────────
    // 1. Write MultipartFile to a temp file (Cloudinary SDK needs File, not stream)
    // 2. Upload to Cloudinary under folder "connectsphere/posts/"
    // 3. Save Media entity with the returned secure_url and public_id
    // 4. Clean up the temp file

    @Override
    public Media uploadMedia(MultipartFile file, int uploaderId, Integer linkedPostId)
            throws IOException {

        // Determine folder and resource_type from MIME type
        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String resourceType = mimeType.startsWith("video/") ? "video" : "image";
        String mediaType   = mimeType.startsWith("video/") ? "VIDEO" : "IMAGE";

        // Upload to Cloudinary
        File tempFile = File.createTempFile("cs_upload_", "_" + file.getOriginalFilename());
        try {
            file.transferTo(tempFile);
            Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                    "folder",        "connectsphere/posts/",
                    "resource_type", resourceType
            ));

            String secureUrl = uploadResult.get("secure_url").toString();
            String publicId  = uploadResult.get("public_id").toString();
            long   bytes     = file.getSize();
            long   sizeKb    = bytes / 1024;

            Media media = new Media();
            media.setUploaderId(uploaderId);
            media.setUrl(secureUrl);
            media.setPublicId(publicId);
            media.setMediaType(mediaType);
            media.setSizeKb(sizeKb);
            media.setMimeType(mimeType);
            media.setLinkedPostId(linkedPostId);
            media.setDeleted(false);

            return mediaRepository.save(media);

        } finally {
            // Always clean up temp file even if upload fails
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // ─── UPLOAD MULTIPLE ──────────────────────────────────────────────────────

    @Override
    public List<Media> uploadMultiple(List<MultipartFile> files, int uploaderId, int linkedPostId)
            throws IOException {
        List<Media> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadMedia(file, uploaderId, linkedPostId));
        }
        return results;
    }

    // ─── GET MEDIA BY POST ────────────────────────────────────────────────────

    @Override
    public List<Media> getMediaByPost(int linkedPostId) {
        return mediaRepository.findByLinkedPostIdAndIsDeletedFalse(linkedPostId);
    }

    // ─── GET MEDIA BY ID ──────────────────────────────────────────────────────

    @Override
    public Optional<Media> getMediaById(int mediaId) {
        return mediaRepository.findByMediaIdAndIsDeletedFalse(mediaId);
    }

    // ─── DELETE MEDIA ─────────────────────────────────────────────────────────
    // Soft-deletes in DB AND removes from Cloudinary to free storage.

    @Override
    @Transactional
    public void deleteMedia(int mediaId) throws IOException {
        Media media = mediaRepository.findByMediaIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        // Remove from Cloudinary
        if (media.getPublicId() != null) {
            String resourceType = "VIDEO".equals(media.getMediaType()) ? "video" : "image";
            cloudinary.uploader().destroy(media.getPublicId(),
                    ObjectUtils.asMap("resource_type", resourceType));
        }

        // Soft-delete in DB (retained for audit — NFR: 30 days)
        media.setDeleted(true);
        mediaRepository.save(media);
    }

    // ─── SOFT DELETE BY POST ──────────────────────────────────────────────────
    // Called when a post is deleted — soft-deletes all its media records.

    @Override
    @Transactional
    public void softDeleteByPost(int linkedPostId) {
        List<Media> mediaList = mediaRepository.findByLinkedPostIdAndIsDeletedFalse(linkedPostId);
        for (Media media : mediaList) {
            media.setDeleted(true);
        }
        mediaRepository.saveAll(mediaList);
    }

    // ─── CREATE STORY ─────────────────────────────────────────────────────────
    // Upload to Cloudinary under "connectsphere/stories/" and save Story entity.
    // expiresAt is set automatically to createdAt + 24h in @PrePersist.

    @Override
    public Story createStory(MultipartFile file, int authorId, String caption)
            throws IOException {

        String mimeType    = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String resourceType = mimeType.startsWith("video/") ? "video" : "image";
        String mediaType   = mimeType.startsWith("video/") ? "VIDEO" : "IMAGE";

        File tempFile = File.createTempFile("cs_story_", "_" + file.getOriginalFilename());
        try {
            file.transferTo(tempFile);
            Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                    "folder",        "connectsphere/stories/",
                    "resource_type", resourceType
            ));

            String secureUrl = uploadResult.get("secure_url").toString();
            String publicId  = uploadResult.get("public_id").toString();

            Story story = new Story();
            story.setAuthorId(authorId);
            story.setMediaUrl(secureUrl);
            story.setPublicId(publicId);
            story.setCaption(caption);
            story.setMediaType(mediaType);
            story.setViewsCount(0);
            // expiresAt and createdAt set by @PrePersist

            return storyRepository.save(story);

        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    // ─── GET ACTIVE STORIES BY USER ───────────────────────────────────────────

    @Override
    public List<Story> getActiveStoriesByUser(int authorId) {
        return storyRepository.findByAuthorIdAndIsActiveTrue(authorId);
    }

    // ─── GET ACTIVE STORIES FOR FEED ─────────────────────────────────────────
    // Called with the list of user IDs the requesting user follows.

    @Override
    public List<Story> getActiveStoriesForFeed(List<Integer> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) return List.of();
        return storyRepository.findByAuthorIdInAndIsActiveTrueOrderByCreatedAtDesc(authorIds);
    }

    // ─── VIEW STORY ───────────────────────────────────────────────────────────
    // Increments viewsCount. Author viewing their own story doesn't count.

    @Override
    @Transactional
    public void viewStory(int storyId, int viewerId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));

        if (!story.isActive()) {
            throw new RuntimeException("Story has expired.");
        }

        // Don't count the author's own views
        if (story.getAuthorId() != viewerId) {
            story.setViewsCount(story.getViewsCount() + 1);
            storyRepository.save(story);
        }
    }

    // ─── DELETE STORY ─────────────────────────────────────────────────────────
    // Removes from Cloudinary and hard-deletes from DB.
    // Only the author can delete their story (checked in resource layer).

    @Override
    @Transactional
    public void deleteStory(int storyId, int requestingUserId) throws IOException {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));

        if (story.getAuthorId() != requestingUserId) {
            throw new RuntimeException("Not authorised to delete this story.");
        }

        // Delete from Cloudinary
        if (story.getPublicId() != null) {
            String resourceType = "VIDEO".equals(story.getMediaType()) ? "video" : "image";
            cloudinary.uploader().destroy(story.getPublicId(),
                    ObjectUtils.asMap("resource_type", resourceType));
        }

        storyRepository.deleteById(storyId);
    }

    // ─── EXPIRE OLD STORIES ───────────────────────────────────────────────────
    // Called by StoryExpiryScheduler every 5 minutes.
    // Uses a bulk UPDATE query for efficiency — one DB round-trip instead of N.

    @Override
    @Transactional
    public int expireOldStories() {
        int count = storyRepository.markExpiredStories(LocalDateTime.now());
        if (count > 0) {
            System.out.println("[media-service] Expired " + count + " stories at " + LocalDateTime.now());
        }
        return count;
    }
}
