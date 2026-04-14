package com.connectsphere.media.scheduler;

import com.connectsphere.media.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * StoryExpiryScheduler — purges stories older than 24 hours.
 *
 * Runs every 5 minutes (300,000 ms).
 * Per NFR: "Stories are purged within 5 minutes of their 24-hour expiry
 * via a scheduled cleanup job."
 *
 * Uses a single bulk UPDATE query via StoryRepository.markExpiredStories()
 * so it is efficient even with thousands of stories.
 */
@Component
public class StoryExpiryScheduler {

    @Autowired
    private MediaService mediaService;

    /**
     * fixedDelay = 300000ms (5 minutes).
     * initialDelay = 60000ms (wait 1 minute after startup before first run).
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void expireStories() {
        int expired = mediaService.expireOldStories();
        if (expired > 0) {
            System.out.println("[StoryExpiryScheduler] Marked " + expired
                    + " stories as inactive.");
        }
    }
}
