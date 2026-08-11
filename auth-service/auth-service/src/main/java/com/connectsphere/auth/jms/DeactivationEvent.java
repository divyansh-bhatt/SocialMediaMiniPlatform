package com.connectsphere.auth.jms;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DeactivationEvent implements Serializable {

    private int    userId;
    private String username;
    private String email;
    private String fullName;
    private String eventAt;
    private String eventType = "DEACTIVATED";

    public DeactivationEvent() {}

    public DeactivationEvent(int userId, String username, String email,
                             String fullName, LocalDateTime deactivatedAt) {
        this.userId        = userId;
        this.username      = username;
        this.email         = email;
        this.fullName      = fullName;
        this.eventAt       = deactivatedAt != null ? deactivatedAt.toString() : null;
    }

    public int    getUserId()        { return userId; }
    public String getUsername()      { return username; }
    public String getEmail()         { return email; }
    public String getFullName()      { return fullName; }
    public String getEventAt()       { return eventAt; }
    public String getEventType()     { return eventType; }

    public void setUserId(int userId)               { this.userId = userId; }
    public void setUsername(String username)         { this.username = username; }
    public void setEmail(String email)               { this.email = email; }
    public void setFullName(String fullName)         { this.fullName = fullName; }
    public void setEventAt(String eventAt)            { this.eventAt = eventAt; }
    public void setEventType(String eventType)        { this.eventType = eventType; }

    @Override
    public String toString() {
        return "DeactivationEvent{userId=" + userId + ", username=" + username
                + ", email=" + email + "}";
    }
}
