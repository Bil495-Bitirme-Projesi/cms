package com.bitiriciler32.cms.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represents an IP camera registered in the system.
 *
 * Health-related fields:
 * - detectionEnabled:      admin intent — "should the AI node process this camera?"
 * - streamStatus:          reported by AI node — "can the AI node reach this camera?"
 * - lastHeartbeatAt:       when the AI node last reported status for this camera
 * - lastStatusAt:          when streamStatus last changed (guards against out-of-order messages)
 * - lastOfflineNotifiedAt: when the last OFFLINE push notification was sent (flapping cooldown)
 */
@Entity
@Table(name = "cameras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CameraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String rtspUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean detectionEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Double threshold = 0.5;

    // ── Health fields (set by AI node, not by admin) ──

    /** Stream connectivity as last reported by the AI Inference Subsystem. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamStatus streamStatus = StreamStatus.UNKNOWN;

    /** When the AI node last sent any status report for this camera. */
    private Instant lastHeartbeatAt;

    /** When streamStatus last transitioned (e.g. ONLINE→OFFLINE). Used to discard stale messages. */
    private Instant lastStatusAt;

    /** When the last OFFLINE push notification was sent. Used for flapping cooldown. */
    private Instant lastOfflineNotifiedAt;
}
