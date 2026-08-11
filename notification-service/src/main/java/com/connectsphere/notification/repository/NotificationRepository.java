package com.connectsphere.notification.repository;

import com.connectsphere.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /** All notifications for a recipient, newest first */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(int recipientId);

    /** Only unread or only read notifications for a recipient */
    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(int recipientId, boolean isRead);

    /** Count of unread notifications (for badge in nav bar) */
    int countByRecipientIdAndIsRead(int recipientId, boolean isRead);

    /** All notifications of a specific type (e.g., all MENTION notifications) */
    List<Notification> findByType(String type);

    /**
     * Find notification by the actor + target combination.
     * Used to avoid creating duplicate notifications for the same event
     * (e.g., user likes then unlikes then likes again — we don't want two LIKE notifs).
     */
    List<Notification> findByActorIdAndTargetIdAndType(int actorId, int targetId, String type);

    /** Delete a single notification by ID */
    void deleteByNotificationId(int notificationId);

    /** Delete all notifications for a recipient (bulk clear) */
    void deleteByRecipientId(int recipientId);
}
