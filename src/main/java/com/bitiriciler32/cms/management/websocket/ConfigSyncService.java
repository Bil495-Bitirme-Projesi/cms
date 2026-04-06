package com.bitiriciler32.cms.management.websocket;

import com.bitiriciler32.cms.management.dto.CameraConfigDto;
import com.bitiriciler32.cms.management.dto.CameraDelta;
import com.bitiriciler32.cms.management.dto.ConfigSnapshot;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.event.CameraConfigChangedEvent;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles configuration synchronization between CMS and the AI Inference Node.
 * - Snapshot sync: sends the full camera configuration on startup/reconnect.
 * - Delta updates: sends incremental changes when cameras are added/updated/deleted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigSyncService {

    private final CameraRepository cameraRepository;
    private final InferenceWsSender wsSender;

    /**
     * Send complete configuration snapshot to a specific session.
     */
    public void sendSnapshot(String sessionId) {
        List<CameraEntity> cameras = cameraRepository.findAllByDeletedFalse();
        List<CameraConfigDto> dtos = cameras.stream()
                .map(this::toConfigDto)
                .collect(Collectors.toList());

        ConfigSnapshot snapshot = new ConfigSnapshot(dtos);
        wsSender.send(sessionId, snapshot);
        log.info("Sent config snapshot ({} cameras) to session {}", dtos.size(), sessionId);
    }

    /**
     * Listen for CameraConfigChangedEvent and push delta to all connected AI nodes.
     *
     * Uses @TransactionalEventListener(AFTER_COMMIT) so that:
     *   - Delta is only sent after the camera change is committed to DB
     *   - WebSocket failures do NOT roll back the camera CRUD operation
     *
     * NOT @Async because:
     *   - WebSocket broadcast is very lightweight (in-memory)
     *   - Ordering of deltas must be preserved (UPSERT before DELETE etc.)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendDelta(CameraConfigChangedEvent event) {
        try {
            CameraDelta delta;
            if ("DELETE".equals(event.getChangeType())) {
                delta = new CameraDelta("DELETE", null, event.getCameraId());
            } else {
                CameraEntity camera = cameraRepository.findByIdAndDeletedFalse(event.getCameraId()).orElse(null);
                if (camera == null) {
                    log.warn("Camera {} not found for delta sync", event.getCameraId());
                    return;
                }
                delta = new CameraDelta("UPSERT", toConfigDto(camera), event.getCameraId());
            }
            wsSender.broadcast(delta);
            log.info("Broadcast camera delta: {} for cameraId={}", event.getChangeType(), event.getCameraId());
        } catch (Exception e) {
            // Log and swallow — DB changes are already committed.
            // AI node will get the correct state on next reconnect via snapshot sync.
            log.error("Failed to broadcast camera delta for cameraId={}: {}",
                    event.getCameraId(), e.getMessage(), e);
        }
    }

    private CameraConfigDto toConfigDto(CameraEntity camera) {
        return new CameraConfigDto(
                camera.getId(),
                camera.getName(),
                camera.getRtspUrl(),
                camera.getDetectionEnabled(),
                camera.getThreshold()
        );
    }
}
