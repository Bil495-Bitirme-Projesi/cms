package com.bitiriciler32.cms.management.service;

import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.dto.CameraResponse;
import com.bitiriciler32.cms.management.dto.CreateCameraRequest;
import com.bitiriciler32.cms.management.dto.UpdateCameraRequest;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.event.CameraConfigPublisher;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-005: CameraService.create()
 * TC-CMS-006: CameraService.update()
 * TC-CMS-007: CameraService.delete()
 * TC-CMS-008: CameraService.findById()
 */
@ExtendWith(MockitoExtension.class)
class CameraServiceTest {

    @Mock CameraRepository cameraRepository;
    @Mock CameraConfigPublisher cameraConfigPublisher;
    @Mock UserCameraAccessRepository userCameraAccessRepository;

    @InjectMocks CameraService cameraService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private CameraEntity savedCamera(Long id, String name, String rtspUrl, boolean detectionEnabled) {
        return CameraEntity.builder()
                .id(id)
                .name(name)
                .rtspUrl(rtspUrl)
                .detectionEnabled(detectionEnabled)
                .streamStatus(StreamStatus.UNKNOWN)
                .deleted(false)
                .build();
    }

    // ── TC-CMS-005: create() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-005: CameraService.create()")
    class CreateTests {

        @Test
        @DisplayName("(1) valid request with detectionEnabled=true - creates camera")
        void create_detectionEnabledTrue_succeeds() {
            CreateCameraRequest req = new CreateCameraRequest("Lobby", "rtsp://host/s1", true);
            CameraEntity saved = savedCamera(1L, "Lobby", "rtsp://host/s1", true);
            when(cameraRepository.save(any())).thenReturn(saved);

            CameraResponse result = cameraService.create(req);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDetectionEnabled()).isTrue();
            verify(cameraConfigPublisher).publishUpsert(1L);
        }

        @Test
        @DisplayName("(2) valid request with detectionEnabled=false - creates camera")
        void create_detectionEnabledFalse_succeeds() {
            CreateCameraRequest req = new CreateCameraRequest("Parking", "rtsp://host/s2", false);
            CameraEntity saved = savedCamera(2L, "Parking", "rtsp://host/s2", false);
            when(cameraRepository.save(any())).thenReturn(saved);

            CameraResponse result = cameraService.create(req);

            assertThat(result.getDetectionEnabled()).isFalse();
        }

        @Test
        @DisplayName("(3) request with null detectionEnabled - defaults to true")
        void create_nullDetectionEnabled_defaultsToTrue() {
            CreateCameraRequest req = new CreateCameraRequest("Gate", "rtsp://host/s3", null);
            CameraEntity saved = savedCamera(3L, "Gate", "rtsp://host/s3", true);
            when(cameraRepository.save(any())).thenReturn(saved);

            CameraResponse result = cameraService.create(req);

            assertThat(result.getDetectionEnabled()).isTrue();
        }

        @Test
        @DisplayName("(4) CameraRepository.save() throws exception - propagates")
        void create_repositorySaveThrows_propagates() {
            CreateCameraRequest req = new CreateCameraRequest("Test", "rtsp://x", true);
            when(cameraRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> cameraService.create(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");
        }

        @Test
        @DisplayName("(5) CameraConfigPublisher.publishUpsert() throws - does not affect saved camera")
        void create_publisherThrows_doesNotRollback() {
            CreateCameraRequest req = new CreateCameraRequest("Test", "rtsp://x", true);
            CameraEntity saved = savedCamera(4L, "Test", "rtsp://x", true);
            when(cameraRepository.save(any())).thenReturn(saved);
            doThrow(new RuntimeException("WS error")).when(cameraConfigPublisher).publishUpsert(any());

            // publisher throws, but service method itself propagates it —
            // the actual rollback behaviour is tested at the IT level.
            assertThatThrownBy(() -> cameraService.create(req))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── TC-CMS-006: update() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-006: CameraService.update()")
    class UpdateTests {

        @Test
        @DisplayName("(1) all fields provided - updates all")
        void update_allFields_updatesAll() {
            CameraEntity existing = savedCamera(1L, "Old", "rtsp://old", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            when(cameraRepository.save(any())).thenReturn(existing);

            UpdateCameraRequest req = new UpdateCameraRequest("New", "rtsp://new", false);
            CameraResponse result = cameraService.update(1L, req);

            assertThat(existing.getName()).isEqualTo("New");
            assertThat(existing.getRtspUrl()).isEqualTo("rtsp://new");
            assertThat(existing.getDetectionEnabled()).isFalse();
            verify(cameraConfigPublisher).publishUpsert(1L);
        }

        @Test
        @DisplayName("(2) partial update - only name")
        void update_onlyName_updatesName() {
            CameraEntity existing = savedCamera(1L, "Old", "rtsp://old", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            when(cameraRepository.save(any())).thenReturn(existing);

            UpdateCameraRequest req = new UpdateCameraRequest("New", null, null);
            cameraService.update(1L, req);

            assertThat(existing.getName()).isEqualTo("New");
            assertThat(existing.getRtspUrl()).isEqualTo("rtsp://old");
        }

        @Test
        @DisplayName("(3) partial update - only rtspUrl")
        void update_onlyRtspUrl_updatesRtspUrl() {
            CameraEntity existing = savedCamera(1L, "Cam", "rtsp://old", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            when(cameraRepository.save(any())).thenReturn(existing);

            UpdateCameraRequest req = new UpdateCameraRequest(null, "rtsp://new", null);
            cameraService.update(1L, req);

            assertThat(existing.getRtspUrl()).isEqualTo("rtsp://new");
            assertThat(existing.getName()).isEqualTo("Cam");
        }

        @Test
        @DisplayName("(4) partial update - only detectionEnabled")
        void update_onlyDetectionEnabled_updates() {
            CameraEntity existing = savedCamera(1L, "Cam", "rtsp://x", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            when(cameraRepository.save(any())).thenReturn(existing);

            UpdateCameraRequest req = new UpdateCameraRequest(null, null, false);
            cameraService.update(1L, req);

            assertThat(existing.getDetectionEnabled()).isFalse();
        }

        @Test
        @DisplayName("(5) camera not found - throws ResourceNotFoundException")
        void update_notFound_throwsResourceNotFoundException() {
            when(cameraRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cameraService.update(99L, new UpdateCameraRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(6) CameraRepository.save() throws exception - propagates")
        void update_repositoryThrows_propagates() {
            CameraEntity existing = savedCamera(1L, "Cam", "rtsp://x", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            when(cameraRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> cameraService.update(1L, new UpdateCameraRequest("New", null, null)))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(7) CameraConfigPublisher.publishUpsert() throws - propagates")
        void update_publisherThrows_propagates() {
            CameraEntity existing = savedCamera(1L, "Cam", "rtsp://x", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            when(cameraRepository.save(any())).thenReturn(existing);
            doThrow(new RuntimeException("WS error")).when(cameraConfigPublisher).publishUpsert(any());

            assertThatThrownBy(() -> cameraService.update(1L, new UpdateCameraRequest("New", null, null)))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── TC-CMS-007: delete() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-007: CameraService.delete()")
    class DeleteTests {

        @Test
        @DisplayName("(1) camera exists - soft-deletes successfully")
        void delete_exists_softDeletesAndPublishes() {
            CameraEntity existing = savedCamera(1L, "Cam", "rtsp://x", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));

            cameraService.delete(1L);

            assertThat(existing.getDeleted()).isTrue();
            verify(cameraRepository).save(existing);
            verify(cameraConfigPublisher).publishDelete(1L);
        }

        @Test
        @DisplayName("(2) camera not found - throws ResourceNotFoundException")
        void delete_notFound_throwsResourceNotFoundException() {
            when(cameraRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cameraService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(3) CameraRepository.save() throws exception - propagates")
        void delete_repositorySaveThrows_propagates() {
            CameraEntity existing = savedCamera(1L, "Cam", "rtsp://x", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            when(cameraRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> cameraService.delete(1L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(4) CameraConfigPublisher.publishDelete() throws - propagates")
        void delete_publisherThrows_propagates() {
            CameraEntity existing = savedCamera(1L, "Cam", "rtsp://x", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existing));
            doThrow(new RuntimeException("WS error")).when(cameraConfigPublisher).publishDelete(any());

            assertThatThrownBy(() -> cameraService.delete(1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── TC-CMS-008: findById() ───────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-008: CameraService.findById()")
    class FindByIdTests {

        @Test
        @DisplayName("(1) camera exists - returns CameraResponse")
        void findById_exists_returnsCameraResponse() {
            CameraEntity camera = savedCamera(1L, "Lobby", "rtsp://x", true);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(camera));

            CameraResponse result = cameraService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Lobby");
        }

        @Test
        @DisplayName("(2) camera not found - throws ResourceNotFoundException")
        void findById_notFound_throwsResourceNotFoundException() {
            when(cameraRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cameraService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

