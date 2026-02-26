package com.bitiriciler32.cms.anomaly.service;

import com.bitiriciler32.cms.anomaly.dto.EventIngestRequest;
import com.bitiriciler32.cms.anomaly.dto.EventIngestResponse;
import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.event.EventPublisher;
import com.bitiriciler32.cms.anomaly.repository.AnomalyEventRepository;
import com.bitiriciler32.cms.common.exception.DuplicateResourceException;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles anomaly event ingestion from the AI Inference Subsystem.
 * Idempotent: duplicate sourceEventId submissions are rejected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestService {

    private final CameraRepository cameraRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final RecipientResolver recipientResolver;
    private final AlertCommandService alertCommandService;
    private final EventPublisher eventPublisher;

    @Transactional
    public EventIngestResponse ingest(EventIngestRequest request) {
        // Idempotency check
        if (anomalyEventRepository.existsBySourceEventId(request.getSourceEventId())) {
            throw new DuplicateResourceException(
                    "Event with sourceEventId " + request.getSourceEventId() + " already exists");
        }

        // Validate camera exists
        CameraEntity camera = cameraRepository.findById(request.getCameraId())
                .orElseThrow(() -> new ResourceNotFoundException("Camera", request.getCameraId()));

        // Persist anomaly event
        AnomalyEventEntity event = AnomalyEventEntity.builder()
                .sourceEventId(request.getSourceEventId())
                .timestamp(request.getTimestamp())
                .score(request.getScore())
                .severity(request.getSeverity())
                .type(request.getType())
                .modelVersion(request.getModelVersion())
                .clipObjectKey(request.getClipObjectKey())
                .camera(camera)
                .build();

        event = anomalyEventRepository.save(event);

        // Resolve recipients and create per-user alerts
        List<UserEntity> recipients = recipientResolver.resolveRecipients(
                camera, request.getSeverity(), request.getType());
        alertCommandService.createAlerts(event, recipients);

        // Flow handoff to Notification module
        eventPublisher.publishEventCreated(event.getId());

        log.info("Anomaly event ingested: id={}, sourceEventId={}, camera={}, severity={}",
                event.getId(), event.getSourceEventId(), camera.getId(), event.getSeverity());

        return new EventIngestResponse(event.getId(), "CREATED");
    }
}
