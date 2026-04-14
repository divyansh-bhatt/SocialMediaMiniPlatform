package com.connectsphere.notification.resource;

import com.connectsphere.notification.dto.BulkNotificationRequest;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * NotificationResource — REST API for the notification-service.
 *
 * Base path: /notifications
 * Port:      8086
 *
 * Internal endpoints (called by other services, no JWT needed for service-to-service):
 *   POST /notifications/internal/create   — create a notification event
 *
 * User endpoints (JWT required via JwtAuthFilter):
 *   GET  /notifications/recipient/{id}    — get my notifications
 *   GET  /notifications/unread-count/{id} — unread badge count
 *   PUT  /notifications/{id}/read         — mark one as read
 *   PUT  /notifications/recipient/{id}/read-all — mark all as read
 *   DELETE /notifications/{id}            — delete one notification
 *
 * Admin endpoints (JWT + ADMIN role):
 *   GET  /notifications/all               — all notifications in system
 *   POST /notifications/bulk              — broadcast to multiple users
 */
@RestController
@RequestMapping("/notifications")
public class NotificationResource {

    @Autowired
    private NotificationService notificationService;

    // No JWT required — inter-service call only (not exposed to public)
    // Body: full Notification JSON with recipientId, actorId, type, message, etc.

    @PostMapping("/internal/create")
    public ResponseEntity<Notification> createInternal(@RequestBody Notification notification) {
        Notification saved = notificationService.createNotification(notification);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // A user fetches their own notification feed
    // JWT userId must match recipientId (or be ADMIN)

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(
            @PathVariable int recipientId,
            HttpServletRequest request) {

        int tokenUserId = (int) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        if (tokenUserId != recipientId && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(notificationService.getByRecipient(recipientId));
    }

    // Returns the number for the nav-bar badge
    @GetMapping("/unread-count/{recipientId}")
    public ResponseEntity<Integer> getUnreadCount(
            @PathVariable int recipientId,
            HttpServletRequest request) {

        int tokenUserId = (int) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        if (tokenUserId != recipientId && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }

    // Mark a single notification as read

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable int notificationId,
            HttpServletRequest request) {

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }

    // Mark all notifications for a user as read
    @PutMapping("/recipient/{recipientId}/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(
            @PathVariable int recipientId,
            HttpServletRequest request) {

        int tokenUserId = (int) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        if (tokenUserId != recipientId && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }

    // Hard-delete a single notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable int notificationId,
            HttpServletRequest request) {

        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted."));
    }

    // Admin only: view every notification in the system

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(notificationService.getAll());
    }

    // Admin broadcast: send a notification to a list of user IDs
    // Body: { "recipientIds": [1, 2, 3], "message": "Platform update!", "type": "SYSTEM" }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, String>> sendBulk(
            @RequestBody BulkNotificationRequest req,
            HttpServletRequest request) {

        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        notificationService.sendBulkNotification(req.getRecipientIds(), req.getMessage(), req.getType());
        return ResponseEntity.ok(Map.of("message", "Bulk notification sent to "
                + req.getRecipientIds().size() + " users."));
    }



    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
