package com.connectsphere.auth.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * DeactivationProducer — sends a DeactivationEvent onto the JMS queue.
 *
 * Called by AuthServiceImpl.deactivateAccount() after setting isActive=false.
 * The call is fire-and-forget — it returns immediately and the email
 * is sent asynchronously by DeactivationEmailListener.
 */
@Component
public class DeactivationProducer {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${app.jms.deactivation-queue}")
    private String queueName;

    public void sendReactivationRequest(DeactivationEvent event) {
        try {
            event.setEventType("REACTIVATION_REQUEST");
            jmsTemplate.convertAndSend(queueName, event);
            System.out.println("[auth-service] Reactivation request queued for: " + event.getUsername());
        } catch (Exception e) {
            System.err.println("[auth-service] WARNING: Failed to queue reactivation request: " + e.getMessage());
        }
    }

    public void sendReactivationApproved(DeactivationEvent event) {
        try {
            event.setEventType("REACTIVATED");
            jmsTemplate.convertAndSend(queueName, event);
            System.out.println("[auth-service] Reactivation confirmation queued for: "
                    + event.getUsername() + " (" + event.getEmail() + ")");
        } catch (Exception e) {
            System.err.println("[auth-service] WARNING: Failed to queue reactivation confirmation for "
                    + event.getUsername() + ": " + e.getMessage());
        }
    }

    public void sendDeactivationEvent(DeactivationEvent event) {
        try {
            event.setEventType("DEACTIVATED");
            jmsTemplate.convertAndSend(queueName, event);
            System.out.println("[auth-service] DeactivationEvent queued for user: "
                    + event.getUsername() + " (" + event.getEmail() + ")");
        } catch (Exception e) {
            // Email failure must never block account deactivation
            System.err.println("[auth-service] WARNING: Failed to queue deactivation event for "
                    + event.getUsername() + ": " + e.getMessage());
        }
    }
}
