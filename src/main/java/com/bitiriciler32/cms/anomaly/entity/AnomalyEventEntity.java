package com.bitiriciler32.cms.anomaly.entity;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Persisted anomaly event metadata received from the AI Inference Subsystem.
 * sourceEventId has a UNIQUE constraint for idempotent ingestion (prevents duplicates on retry).
 */
@Entity
@Table(name = "anomaly_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique identifier from the AI node to ensure idempotent ingestion. */
    @Column(nullable = false, unique = true)
    private String sourceEventId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String type;

    private String modelVersion;

    /** Object key referencing the video clip stored in MinIO. */
    private String clipObjectKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camera_id", nullable = false)
    private CameraEntity camera;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
