package com.bitiriciler32.cms.anomaly.entity;

import com.bitiriciler32.cms.management.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Per-user alert record. One UserAlertEntity is created for each recipient user
 * when an anomaly event is ingested (per LLD design decision: per-user duplication
 * for independent lifecycle tracking).
 */
@Entity
@Table(name = "user_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status = AlertStatus.UNSEEN;

    private Instant readAt;

    private Instant ackAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean falsePositive = false;

    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AnomalyEventEntity event;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
