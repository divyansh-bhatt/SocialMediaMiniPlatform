package com.connectsphere.notification.service;

import com.connectsphere.notification.entity.Notification;

import java.util.List;

public interface NotificationService {


    Notification createNotification(Notification notification);

    void sendBulkNotification(List<Integer> recipientIds, String message, String type);

    void markAsRead(int notificationId);

    void markAllRead(int recipientId);

    List<Notification> getByRecipient(int recipientId);

    int getUnreadCount(int recipientId);

    void deleteNotification(int notificationId);

    void sendEmailAlert(String toEmail, String subject, String body);
    List<Notification> getAll();
}
