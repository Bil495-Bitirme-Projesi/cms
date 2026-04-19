package com.bitiriciler32.cms.notification.entity;

import com.bitiriciler32.cms.management.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Stores Firebase Cloud Messaging device tokens registered by mobile clients.
 */
@Entity
@Table(name = "device_fcm_tokens", uniqueConstraints = @UniqueConstraint(name = "uq_device_fcm_tokens_user", columnNames = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceFcmTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(nullable = false)
    private String fcmToken;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = Instant.now();
    }
}
