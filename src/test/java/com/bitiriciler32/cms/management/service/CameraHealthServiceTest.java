package com.bitiriciler32.cms.management.service;

import com.bitiriciler32.cms.management.dto.CameraStatusReport;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.event.CameraOfflineEvent;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-009: CameraHealthService.applyStatusReport()
 */
@ExtendWith(MockitoExtension.class)
class CameraHealthServiceTest {

    @Mock CameraRepository cameraRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CameraHealthService cameraHealthService;

    private CameraEntity camera(StreamStatus status, Instant lastStatusAt, Instant lastOfflineNotifiedAt) {
        CameraEntity c = CameraEntity.builder()
                .id(1L)
                .name("Test Camera")
                .rtspUrl("rtsp://x")
                .detectionEnabled(true)
                .streamStatus(status)
                .lastStatusAt(lastStatusAt)
                .lastOfflineNotifiedAt(lastOfflineNotifiedAt)
                .deleted(false)
                .build();
        return c;
    }

    private CameraStatusReport report(long cameraId, String status, Instant reportedAt) {
        CameraStatusReport r = new CameraStatusReport();
        r.setCameraId(cameraId);
        r.setStatus(status);
        r.setReportedAt(reportedAt);
        return r;
    }

    @Nested
    @DisplayName("TC-CMS-009: CameraHealthService.applyStatusReport()")
    class ApplyStatusReportTests {

        @Test
        @DisplayName("(1) camera not found - returns false (silently discards)")
        void applyStatusReport_cameraNotFound_returnsFalse() {
            when(cameraRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            boolean result = cameraHealthService.applyStatusReport(report(99L, "ONLINE", Instant.now()));

            assertThat(result).isFalse();
            verify(cameraRepository, never()).save(any());
        }

        @Test
        @DisplayName("(2) first status report (lastStatusAt is null) - updates status, returns true")
        void applyStatusReport_firstReport_updatesStatus() {
            CameraEntity cam = camera(StreamStatus.UNKNOWN, null, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            boolean result = cameraHealthService.applyStatusReport(report(1L, "ONLINE", Instant.now()));

            assertThat(result).isTrue();
            assertThat(cam.getStreamStatus()).isEqualTo(StreamStatus.ONLINE);
            verify(cameraRepository, atLeastOnce()).save(cam);
        }

        @Test
        @DisplayName("(3) newer report (reportedAt > lastStatusAt) - updates status")
        void applyStatusReport_newerReport_updatesStatus() {
            Instant past = Instant.now().minusSeconds(60);
            CameraEntity cam = camera(StreamStatus.ONLINE, past, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            Instant now = Instant.now();
            boolean result = cameraHealthService.applyStatusReport(report(1L, "OFFLINE", now));

            assertThat(result).isTrue();
            assertThat(cam.getStreamStatus()).isEqualTo(StreamStatus.OFFLINE);
        }

        @Test
        @DisplayName("(4) out-of-order report (reportedAt < lastStatusAt) - discards, returns false")
        void applyStatusReport_outOfOrder_discards() {
            Instant recent = Instant.now();
            CameraEntity cam = camera(StreamStatus.ONLINE, recent, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            Instant stale = recent.minusSeconds(120);
            boolean result = cameraHealthService.applyStatusReport(report(1L, "OFFLINE", stale));

            assertThat(result).isFalse();
            assertThat(cam.getStreamStatus()).isEqualTo(StreamStatus.ONLINE); // unchanged
        }

        @Test
        @DisplayName("(5) same status ONLINE → ONLINE - updates heartbeat only, returns false")
        void applyStatusReport_sameStatus_updatesHeartbeatOnly() {
            Instant past = Instant.now().minusSeconds(30);
            CameraEntity cam = camera(StreamStatus.ONLINE, past, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            Instant now = Instant.now();
            boolean result = cameraHealthService.applyStatusReport(report(1L, "ONLINE", now));

            assertThat(result).isFalse();
            assertThat(cam.getStreamStatus()).isEqualTo(StreamStatus.ONLINE);
            assertThat(cam.getLastHeartbeatAt()).isEqualTo(now);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("(6) status transition UNKNOWN → ONLINE - updates status, returns true")
        void applyStatusReport_unknownToOnline_returnsTrue() {
            CameraEntity cam = camera(StreamStatus.UNKNOWN, null, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            boolean result = cameraHealthService.applyStatusReport(report(1L, "ONLINE", Instant.now()));

            assertThat(result).isTrue();
            assertThat(cam.getStreamStatus()).isEqualTo(StreamStatus.ONLINE);
        }

        @Test
        @DisplayName("(7) transition ONLINE → OFFLINE (no previous notification) - publishes CameraOfflineEvent")
        void applyStatusReport_onlineToOfflineNoPreviousNotification_publishesEvent() {
            Instant past = Instant.now().minusSeconds(60);
            CameraEntity cam = camera(StreamStatus.ONLINE, past, null); // no lastOfflineNotifiedAt
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            cameraHealthService.applyStatusReport(report(1L, "OFFLINE", Instant.now()));

            ArgumentCaptor<CameraOfflineEvent> captor = ArgumentCaptor.forClass(CameraOfflineEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getCameraId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("(8) transition ONLINE → OFFLINE (within cooldown) - suppresses notification")
        void applyStatusReport_withinCooldown_suppressesNotification() {
            Instant past = Instant.now().minusSeconds(60);
            // Last notification was 2 minutes ago — within the 5-minute cooldown
            Instant recentNotification = Instant.now().minusSeconds(120);
            CameraEntity cam = camera(StreamStatus.ONLINE, past, recentNotification);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            cameraHealthService.applyStatusReport(report(1L, "OFFLINE", Instant.now()));

            verify(eventPublisher, never()).publishEvent(any());
            assertThat(cam.getStreamStatus()).isEqualTo(StreamStatus.OFFLINE);
        }

        @Test
        @DisplayName("(9) transition ONLINE → OFFLINE (outside cooldown) - publishes event")
        void applyStatusReport_outsideCooldown_publishesEvent() {
            Instant past = Instant.now().minusSeconds(60);
            // Last notification was 10 minutes ago — outside the 5-minute cooldown
            Instant oldNotification = Instant.now().minusSeconds(600);
            CameraEntity cam = camera(StreamStatus.ONLINE, past, oldNotification);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            cameraHealthService.applyStatusReport(report(1L, "OFFLINE", Instant.now()));

            verify(eventPublisher).publishEvent(any(CameraOfflineEvent.class));
        }

        @Test
        @DisplayName("(10) transition OFFLINE → ONLINE - updates status, returns true")
        void applyStatusReport_offlineToOnline_returnsTrue() {
            Instant past = Instant.now().minusSeconds(60);
            CameraEntity cam = camera(StreamStatus.OFFLINE, past, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            boolean result = cameraHealthService.applyStatusReport(report(1L, "ONLINE", Instant.now()));

            assertThat(result).isTrue();
            assertThat(cam.getStreamStatus()).isEqualTo(StreamStatus.ONLINE);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("(11) report with null reportedAt - uses Instant.now() (does not throw)")
        void applyStatusReport_nullReportedAt_usesNow() {
            CameraEntity cam = camera(StreamStatus.UNKNOWN, null, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            CameraStatusReport r = report(1L, "ONLINE", null);
            assertThatCode(() -> cameraHealthService.applyStatusReport(r))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("(12) invalid status string - throws IllegalArgumentException")
        void applyStatusReport_invalidStatus_throwsIllegalArgumentException() {
            Instant past = Instant.now().minusSeconds(60);
            CameraEntity cam = camera(StreamStatus.UNKNOWN, null, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));

            assertThatThrownBy(() -> cameraHealthService.applyStatusReport(report(1L, "INVALID", Instant.now())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("(13) ApplicationEventPublisher throws - exception propagates")
        void applyStatusReport_publisherThrows_propagates() {
            Instant past = Instant.now().minusSeconds(60);
            CameraEntity cam = camera(StreamStatus.ONLINE, past, null);
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            doThrow(new RuntimeException("event error")).when(eventPublisher).publishEvent(any());

            assertThatThrownBy(() -> cameraHealthService.applyStatusReport(report(1L, "OFFLINE", Instant.now())))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

