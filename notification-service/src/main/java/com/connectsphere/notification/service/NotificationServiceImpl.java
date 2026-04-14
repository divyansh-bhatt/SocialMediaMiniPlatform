package com.connectsphere.notification.service;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * It is declared as required=false so the service still starts even when
     * SMTP credentials are not yet configured (email calls will be no-ops).
     */
    @Autowired(required = false)
    private JavaMailSender emailSender;

    @Override
    public Notification createNotification(Notification notification) {
        if (notification.getRecipientId() == notification.getActorId()) {
            return notification;
        }
        return notificationRepository.save(notification);
    }

    // Used by admin broadcast and system alerts.
    // Creates one notification row per recipientId.

    @Override
    @Transactional
    public void sendBulkNotification(List<Integer> recipientIds, String message, String type) {
        for (int recipientId : recipientIds) {
            Notification n = new Notification();
            n.setRecipientId(recipientId);
            // actorId = 0 signals a system/admin-generated notification
            n.setActorId(0);
            n.setType(type);
            n.setMessage(message);
            n.setRead(false);
            notificationRepository.save(n);
        }
    }

    @Override
    @Transactional
    public void markAsRead(int notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void markAllRead(int recipientId) {
        List<Notification> unread =
                notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, false);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    public List<Notification> getByRecipient(int recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }


    @Override
    public int getUnreadCount(int recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    @Transactional
    public void deleteNotification(int notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new RuntimeException("Notification not found: " + notificationId);
        }
        notificationRepository.deleteByNotificationId(notificationId);
    }

    // Used for high-priority events: account actions, follower milestones, etc.
    // Gracefully degrades if SMTP is not configured.

    @Override
    public void sendEmailAlert(String toEmail, String subject, String body) {
        if (emailSender == null) {
            System.err.println("[notification-service] WARNING: JavaMailSender not configured. " +
                    "Email to " + toEmail + " not sent. Configure spring.mail.* properties.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            emailSender.send(message);
        } catch (Exception e) {
            System.err.println("[notification-service] WARNING: Failed to send email to "
                    + toEmail + ": " + e.getMessage());
        }
    }

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }
}
