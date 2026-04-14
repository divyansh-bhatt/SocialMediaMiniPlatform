package com.connectsphere.notification.dto;

import lombok.Data;

import java.util.List;

/**
 * Request body for POST /notifications/bulk
 * Used by admins to broadcast a message to multiple users.
 */
@Data
public class BulkNotificationRequest {

    /** List of user IDs who should receive the notification */
    private List<Integer> recipientIds;

    /** The notification message text */
    private String message;

    /**
     * Notification type — for admin/system broadcasts use "SYSTEM".
     * Standard types: LIKE | COMMENT | REPLY | FOLLOW | MENTION | SYSTEM
     */
    private String type;
}
