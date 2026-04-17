package com.bitiriciler32.cms.anomaly.service;

import com.bitiriciler32.cms.anomaly.dto.EventIngestRequest;
import com.bitiriciler32.cms.anomaly.dto.EventIngestResponse;
import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.event.EventPublisher;
import com.bitiriciler32.cms.anomaly.repository.AnomalyEventRepository;
import com.bitiriciler32.cms.common.exception.DuplicateResourceException;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.media.dto.UploadUrlResponse;
import com.bitiriciler32.cms.media.service.ClipStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-015: EventIngestService.ingest()
 */
@ExtendWith(MockitoExtension.class)
class EventIngestServiceTest {

    @Mock CameraRepository cameraRepository;
    @Mock AnomalyEventRepository anomalyEventRepository;
    @Mock RecipientResolver recipientResolver;
    @Mock AlertCommandService alertCommandService;
    @Mock EventPublisher eventPublisher;
    @Mock ClipStorageService clipStorageService;

    @InjectMocks EventIngestService eventIngestService;

    private CameraEntity camera(Long id) {
        return CameraEntity.builder()
                .id(id).name("Cam").rtspUrl("rtsp://x")
                .detectionEnabled(true).streamStatus(StreamStatus.UNKNOWN)
                .deleted(false).build();
    }

    private UserEntity user(Long id) {
        return UserEntity.builder()
                .id(id).name("User").email("u@test.com")
                .passwordHash("hash").role(Role.OPERATOR).enabled(true).build();
    }

    private AnomalyEventEntity savedEvent(Long id, CameraEntity camera) {
        return AnomalyEventEntity.builder()
                .id(id).sourceEventId("src-1")
                .timestamp(Instant.now()).score(0.9)
                .type("INTRUSION").camera(camera).build();
    }

    private EventIngestRequest validRequest() {
        return new EventIngestRequest("src-1", 1L, Instant.now(), 0.9, "INTRUSION", "desc");
    }

    @Nested
    @DisplayName("TC-CMS-015: EventIngestService.ingest()")
    class IngestTests {

        @Test
        @DisplayName("(1) valid request, camera exists, has recipients - creates event and alerts")
        void ingest_validWithRecipients_createsEventAndAlerts() {
            CameraEntity cam = camera(1L);
            AnomalyEventEntity event = savedEvent(10L, cam);
            UserEntity recipient = user(1L);

            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(false);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(anomalyEventRepository.save(any())).thenReturn(event);
            when(clipStorageService.generateUploadUrl(anyLong(), anyLong()))
                    .thenReturn(new UploadUrlResponse("cameras/1/events/10.mp4", "http://upload", 300L));
            when(recipientResolver.resolveRecipients(any(), any())).thenReturn(List.of(recipient));

            EventIngestResponse response = eventIngestService.ingest(validRequest());

            assertThat(response.getEventId()).isEqualTo(10L);
            verify(alertCommandService).createAlerts(event, List.of(recipient));
            verify(eventPublisher).publishEventCreated(10L);
        }

        @Test
        @DisplayName("(2) valid request, camera exists, no recipients - creates event but no alerts")
        void ingest_validNoRecipients_createsEventNoAlerts() {
            CameraEntity cam = camera(1L);
            AnomalyEventEntity event = savedEvent(11L, cam);

            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(false);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(anomalyEventRepository.save(any())).thenReturn(event);
            when(clipStorageService.generateUploadUrl(anyLong(), anyLong()))
                    .thenReturn(new UploadUrlResponse("key", "http://upload", 300L));
            when(recipientResolver.resolveRecipients(any(), any())).thenReturn(List.of());

            eventIngestService.ingest(validRequest());

            verify(alertCommandService).createAlerts(event, List.of());
        }

        @Test
        @DisplayName("(3) duplicate sourceEventId - throws DuplicateResourceException")
        void ingest_duplicateSourceEventId_throwsDuplicateResourceException() {
            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(true);

            assertThatThrownBy(() -> eventIngestService.ingest(validRequest()))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("(4) camera not found - throws ResourceNotFoundException")
        void ingest_cameraNotFound_throwsResourceNotFoundException() {
            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(false);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventIngestService.ingest(validRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(5) AnomalyEventRepository.save() throws - propagates")
        void ingest_repositorySaveThrows_propagates() {
            CameraEntity cam = camera(1L);
            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(false);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(anomalyEventRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> eventIngestService.ingest(validRequest()))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(6) RecipientResolver.resolveRecipients() throws - propagates")
        void ingest_recipientResolverThrows_propagates() {
            CameraEntity cam = camera(1L);
            AnomalyEventEntity event = savedEvent(10L, cam);
            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(false);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(anomalyEventRepository.save(any())).thenReturn(event);
            when(clipStorageService.generateUploadUrl(anyLong(), anyLong()))
                    .thenReturn(new UploadUrlResponse("key", "http://upload", 300L));
            when(recipientResolver.resolveRecipients(any(), any()))
                    .thenThrow(new RuntimeException("resolve error"));

            assertThatThrownBy(() -> eventIngestService.ingest(validRequest()))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(7) AlertCommandService.createAlerts() throws - propagates (should rollback)")
        void ingest_createAlertsThrows_propagates() {
            CameraEntity cam = camera(1L);
            AnomalyEventEntity event = savedEvent(10L, cam);
            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(false);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(anomalyEventRepository.save(any())).thenReturn(event);
            when(clipStorageService.generateUploadUrl(anyLong(), anyLong()))
                    .thenReturn(new UploadUrlResponse("key", "http://upload", 300L));
            when(recipientResolver.resolveRecipients(any(), any())).thenReturn(List.of());
            doThrow(new RuntimeException("alert error")).when(alertCommandService).createAlerts(any(), any());

            assertThatThrownBy(() -> eventIngestService.ingest(validRequest()))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(8) EventPublisher.publishEventCreated() throws - propagates")
        void ingest_publisherThrows_propagates() {
            CameraEntity cam = camera(1L);
            AnomalyEventEntity event = savedEvent(10L, cam);
            when(anomalyEventRepository.existsBySourceEventId("src-1")).thenReturn(false);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(anomalyEventRepository.save(any())).thenReturn(event);
            when(clipStorageService.generateUploadUrl(anyLong(), anyLong()))
                    .thenReturn(new UploadUrlResponse("key", "http://upload", 300L));
            when(recipientResolver.resolveRecipients(any(), any())).thenReturn(List.of());
            doThrow(new RuntimeException("publish error")).when(eventPublisher).publishEventCreated(anyLong());

            assertThatThrownBy(() -> eventIngestService.ingest(validRequest()))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

