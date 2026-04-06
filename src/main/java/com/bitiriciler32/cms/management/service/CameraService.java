package com.bitiriciler32.cms.management.service;

import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.dto.CameraResponse;
import com.bitiriciler32.cms.management.dto.CreateCameraRequest;
import com.bitiriciler32.cms.management.dto.UpdateCameraRequest;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.event.CameraConfigPublisher;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CameraService {

    private final CameraRepository cameraRepository;
    private final CameraConfigPublisher cameraConfigPublisher;
    private final UserCameraAccessRepository userCameraAccessRepository;

    @Transactional
    public CameraResponse create(CreateCameraRequest request) {
        CameraEntity camera = CameraEntity.builder()
                .name(request.getName())
                .rtspUrl(request.getRtspUrl())
                .detectionEnabled(request.getDetectionEnabled() != null ? request.getDetectionEnabled() : true)
                .threshold(request.getThreshold())
                .build();

        camera = cameraRepository.save(camera);

        // Push delta update to AI Inference node via WebSocket
        cameraConfigPublisher.publishUpsert(camera.getId());

        return toResponse(camera);
    }

    @Transactional
    public CameraResponse update(Long id, UpdateCameraRequest request) {
        CameraEntity camera = cameraRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", id));

        if (request.getName() != null) {
            camera.setName(request.getName());
        }
        if (request.getRtspUrl() != null) {
            camera.setRtspUrl(request.getRtspUrl());
        }
        if (request.getDetectionEnabled() != null) {
            camera.setDetectionEnabled(request.getDetectionEnabled());
        }
        if (request.getThreshold() != null) {
            camera.setThreshold(request.getThreshold());
        }

        camera = cameraRepository.save(camera);

        // Push delta update to AI Inference node via WebSocket
        cameraConfigPublisher.publishUpsert(camera.getId());

        return toResponse(camera);
    }

    /**
     * Soft-delete a camera.
     * Sets deleted=true instead of removing the row so that historical anomaly events
     * and user alerts referencing this camera are preserved.
     * Access-control mappings for this camera are hard-deleted since they have no
     * independent historical value.
     */
    @Transactional
    public void delete(Long id) {
        CameraEntity camera = cameraRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", id));

        // Revoke all per-user access mappings — no historical value once the camera is gone.
        userCameraAccessRepository.deleteAllByCamera(camera);

        // Soft delete: preserve the row so FK references in anomaly_events stay valid.
        camera.setDeleted(true);
        cameraRepository.save(camera);

        // Notify AI Inference node — it should stop processing this camera.
        cameraConfigPublisher.publishDelete(id);
    }

    @Transactional(readOnly = true)
    public List<CameraResponse> findAll() {
        return cameraRepository.findAllByDeletedFalse().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CameraResponse findById(Long id) {
        CameraEntity camera = cameraRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", id));
        return toResponse(camera);
    }

    private CameraResponse toResponse(CameraEntity camera) {
        return new CameraResponse(
                camera.getId(),
                camera.getName(),
                camera.getRtspUrl(),
                camera.getDetectionEnabled(),
                camera.getThreshold(),
                camera.getStreamStatus(),
                camera.getLastHeartbeatAt()
        );
    }
}
