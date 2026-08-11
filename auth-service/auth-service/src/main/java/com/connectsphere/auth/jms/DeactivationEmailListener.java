package com.connectsphere.auth.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class DeactivationEmailListener {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @JmsListener(destination = "${app.jms.deactivation-queue}")
    public void handleDeactivationEvent(DeactivationEvent event) {
        String eventType = resolveEventType(event);
        System.out.println("[auth-service] Processing " + eventType + " email event for: "
                + event.getUsername());

        if (mailSender == null) {
            System.err.println("[auth-service] JavaMailSender not configured. "
                    + "Skipping email for " + event.getEmail()
                    + ". Configure spring.mail.* in application.properties.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(event.getEmail());
            message.setSubject(buildSubject(eventType));
            message.setText(buildEmailBody(event, eventType));
            mailSender.send(message);

            System.out.println("[auth-service] " + eventType + " email sent to: " + event.getEmail());
        } catch (Exception e) {
            System.err.println("[auth-service] ERROR: Failed to send " + eventType + " email to "
                    + event.getEmail() + ": " + e.getMessage());
            throw new RuntimeException("Email send failed - will retry", e);
        }
    }

    private String buildSubject(String eventType) {
        return switch (eventType) {
            case "REACTIVATED" -> "Your ConnectSphere account has been reactivated";
            case "REACTIVATION_REQUEST" -> "ConnectSphere account reactivation request received";
            default -> "Your ConnectSphere account has been deactivated";
        };
    }

    private String buildEmailBody(DeactivationEvent event, String eventType) {
        String displayName = event.getFullName() != null && !event.getFullName().isBlank()
                ? event.getFullName()
                : event.getUsername();

        if ("REACTIVATED".equals(eventType)) {
            return """
                    Hi %s,

                    Your ConnectSphere account (@%s) has been reactivated by an admin.

                    You can now log in again and continue using your account.

                    If you did not expect this change, please contact support immediately.

                    - The ConnectSphere Team
                    """.formatted(displayName, event.getUsername());
        }

        if ("REACTIVATION_REQUEST".equals(eventType)) {
            return """
                    Hi %s,

                    We received your request to reactivate your ConnectSphere account (@%s).

                    An admin will review the request. You will receive another email when your account is restored.

                    - The ConnectSphere Team
                    """.formatted(displayName, event.getUsername());
        }

        return """
                Hi %s,

                Your ConnectSphere account (@%s) has been deactivated as requested.

                What this means:
                  - You can no longer log in to your account
                  - Your posts and profile are no longer visible to others
                  - Your data is retained for 30 days per our data policy

                If this was a mistake or you want to restore your account,
                please contact support at support@connectsphere.com and we will
                restore your account within 24 hours.

                If you did not request this, please contact support immediately.

                - The ConnectSphere Team
                """.formatted(displayName, event.getUsername());
    }

    private String resolveEventType(DeactivationEvent event) {
        if (event.getEventType() != null && !event.getEventType().isBlank()) {
            return event.getEventType();
        }
        if (event.getUsername() != null && event.getUsername().startsWith("REACTIVATION_REQUEST:")) {
            event.setUsername(event.getUsername().substring("REACTIVATION_REQUEST:".length()));
            return "REACTIVATION_REQUEST";
        }
        return "DEACTIVATED";
    }
}
