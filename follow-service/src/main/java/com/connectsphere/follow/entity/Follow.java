package com.connectsphere.follow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows",
    uniqueConstraints = {
        // Prevent duplicate follow records at DB level
        // One followerId+followeeId pair only
        @UniqueConstraint(columnNames = {"follower_id", "followee_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private int followId;

    // The person who clicked Follow
    @Column(name = "follower_id", nullable = false)
    private int followerId;

    // The person being followed
    @Column(name = "followee_id", nullable = false)
    private int followeeId;

    // ACTIVE — normal public account follow
    // PENDING — followee has a private account, waiting for approval
    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }
}
