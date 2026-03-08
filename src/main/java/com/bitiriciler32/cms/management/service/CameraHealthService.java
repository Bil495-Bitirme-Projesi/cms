package com.bitiriciler32.cms.management.service;

import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.dto.CameraStatusReport;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.event.CameraOfflineEvent;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Processes camera health status reports from the AI Inference Subsystem.
 *
 * Two distinct protections:
 *
 * 1. Out-of-order guard:
 *    Compares the AI-side reportedAt timestamp against the stored lastStatusAt.
 *    If the incoming report is older, it is discarded.
 *    (Currently defensive — WebSocket/TCP guarantees ordering for a single connection,
 *     but this protects against future multi-node scenarios.)
 *
 * 2. Flapping cooldown:
 *    A camera with an unstable network may rapidly alternate OFFLINE↔ONLINE.
 *    Without protection each OFFLINE transition would trigger a push notification.
 *    We suppress repeated OFFLINE notifications within a configurable cooldown window
 *    (OFFLINE_NOTIFY_COOLDOWN). The status in the DB still reflects reality;
 *    only the push notification is suppressed.
 *
 * Threading note:
 *    applyStatusReport() is called directly on the WebSocket handler thread.
 *    Each call does 1 DB read + 1-3 DB writes (~5-20ms on local Postgres).
 *    At the expected scale (tens of cameras, heartbeat every ~60s) this is negligible.
 *    If camera count grows significantly or heartbeat interval shrinks, consider
 *    dispatching this work to a dedicated thread pool via @Async.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CameraHealthService {

    /**
     * Minimum interval between two OFFLINE push notifications for the same camera.
     * During flapping (rapid OFFLINE↔ONLINE), notifications are suppressed within this window.
     */
    private static final Duration OFFLINE_NOTIFY_COOLDOWN = Duration.ofMinutes(5);

    private final CameraRepository cameraRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Apply a status report from the AI node.
     *
     * @return true if the status was actually updated, false if the report was stale/duplicate.
     */
    @Transactional
    public boolean applyStatusReport(CameraStatusReport report) {
        CameraEntity camera = cameraRepository.findById(report.getCameraId())
                .orElseThrow(() -> new ResourceNotFoundException("Camera", report.getCameraId()));

        Instant reportedAt = report.getReportedAt() != null ? report.getReportedAt() : Instant.now();

        // Always update heartbeat timestamp
        camera.setLastHeartbeatAt(reportedAt);

        // Guard: discard out-of-order messages
        if (camera.getLastStatusAt() != null && reportedAt.isBefore(camera.getLastStatusAt())) {
            log.debug("Discarding stale status report for camera {}: reportedAt={} < lastStatusAt={}",
                    camera.getId(), reportedAt, camera.getLastStatusAt());
            cameraRepository.save(camera);
            return false;
        }

        StreamStatus newStatus = StreamStatus.valueOf(report.getStatus().toUpperCase());
        StreamStatus oldStatus = camera.getStreamStatus();

        // No transition — update heartbeat only
        if (oldStatus == newStatus) {
            cameraRepository.save(camera);
            log.debug("Camera {} status unchanged: {}", camera.getId(), newStatus);
            return false;
        }

        // Status transition — always persist the real status
        camera.setStreamStatus(newStatus);
        camera.setLastStatusAt(reportedAt);
        cameraRepository.save(camera);

        log.info("Camera {} status changed: {} → {}", camera.getId(), oldStatus, newStatus);

        // Notification: only on transition TO OFFLINE, with cooldown
        if (newStatus == StreamStatus.OFFLINE) {
            if (isWithinCooldown(camera.getLastOfflineNotifiedAt())) {
                log.info("Camera {} OFFLINE notification suppressed (cooldown active, last notified at {})",
                        camera.getId(), camera.getLastOfflineNotifiedAt());
            } else {
                camera.setLastOfflineNotifiedAt(Instant.now());
                cameraRepository.save(camera);
                eventPublisher.publishEvent(new CameraOfflineEvent(camera.getId(), camera.getName()));
            }
        }

        return true;
    }

    private boolean isWithinCooldown(Instant lastNotified) {
        if (lastNotified == null) {
            return false;
        }
        return Duration.between(lastNotified, Instant.now()).compareTo(OFFLINE_NOTIFY_COOLDOWN) < 0;
    }
}

