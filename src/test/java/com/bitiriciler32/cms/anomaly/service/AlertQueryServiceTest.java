package com.bitiriciler32.cms.anomaly.service;

import com.bitiriciler32.cms.anomaly.dto.AlertDetailResponse;
import com.bitiriciler32.cms.anomaly.dto.AlertQueryFilter;
import com.bitiriciler32.cms.anomaly.dto.AlertSummaryResponse;
import com.bitiriciler32.cms.anomaly.entity.AlertStatus;
import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-020: AlertQueryService.getAlerts()
 * TC-CMS-021: AlertQueryService.getAlertDetail()
 */
@ExtendWith(MockitoExtension.class)
class AlertQueryServiceTest {

    @Mock UserAlertRepository userAlertRepository;

    @InjectMocks AlertQueryService alertQueryService;

    private UserEntity user() {
        return UserEntity.builder().id(1L).name("U").email("u@t.com")
                .passwordHash("h").role(Role.OPERATOR).enabled(true).build();
    }

    private CameraEntity camera() {
        return CameraEntity.builder().id(1L).name("Cam").rtspUrl("rtsp://x")
                .detectionEnabled(true).streamStatus(StreamStatus.UNKNOWN).deleted(false).build();
    }

    private AnomalyEventEntity event() {
        return AnomalyEventEntity.builder()
                .id(10L).sourceEventId("s1").timestamp(Instant.now())
                .score(0.9).type("INTRUSION").camera(camera()).build();
    }

    private UserAlertEntity alert(Long id, AlertStatus status) {
        return UserAlertEntity.builder()
                .id(id).status(status)
                .user(user()).event(event()).build();
    }

    // ── TC-CMS-020: getAlerts() ──────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-020: AlertQueryService.getAlerts()")
    class GetAlertsTests {

        @Test
        @DisplayName("(1) null filter - returns all alerts for user")
        void getAlerts_nullFilter_returnsAll() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(alert(1L, AlertStatus.UNSEEN), alert(2L, AlertStatus.ACKNOWLEDGED)));

            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("(2) filter with status only - applies status filter")
        void getAlerts_statusFilter_appliesFilter() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(alert(1L, AlertStatus.UNSEEN)));

            AlertQueryFilter filter = new AlertQueryFilter(AlertStatus.UNSEEN, null, null, null, null);
            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), filter);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("(3) filter with cameraId only - applies camera filter")
        void getAlerts_cameraIdFilter_appliesFilter() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(alert(1L, AlertStatus.UNSEEN)));

            AlertQueryFilter filter = new AlertQueryFilter(null, 1L, null, null, null);
            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), filter);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("(4) filter with date range (from and to) - applies date filter")
        void getAlerts_dateRangeFilter_appliesFilter() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(alert(1L, AlertStatus.UNSEEN)));

            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();
            AlertQueryFilter filter = new AlertQueryFilter(null, null, from, to, null);
            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), filter);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("(5) filter with from only - filters from date onwards")
        void getAlerts_fromFilter_appliesFilter() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(alert(1L, AlertStatus.UNSEEN)));

            AlertQueryFilter filter = new AlertQueryFilter(null, null, Instant.now().minusSeconds(3600), null, null);
            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), filter);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("(6) filter with to only - filters up to date")
        void getAlerts_toFilter_appliesFilter() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of());

            AlertQueryFilter filter = new AlertQueryFilter(null, null, null, Instant.now().minusSeconds(3600), null);
            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), filter);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("(7) filter with type - filters by event type")
        void getAlerts_typeFilter_appliesFilter() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(alert(1L, AlertStatus.UNSEEN)));

            AlertQueryFilter filter = new AlertQueryFilter(null, null, null, null, "INTRUSION");
            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), filter);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("(8) filter with multiple criteria - applies all filters")
        void getAlerts_multipleFilters_appliesAll() {
            when(userAlertRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(alert(1L, AlertStatus.ACKNOWLEDGED)));

            AlertQueryFilter filter = new AlertQueryFilter(
                    AlertStatus.ACKNOWLEDGED, 1L, Instant.now().minusSeconds(3600), Instant.now(), "INTRUSION");
            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), filter);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("(9) no alerts match filter - returns empty list")
        void getAlerts_noMatch_returnsEmpty() {
            when(userAlertRepository.findAll(any(Specification.class))).thenReturn(List.of());

            List<AlertSummaryResponse> result = alertQueryService.getAlerts(user(), new AlertQueryFilter());

            assertThat(result).isEmpty();
        }
    }

    // ── TC-CMS-021: getAlertDetail() ─────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-021: AlertQueryService.getAlertDetail()")
    class GetAlertDetailTests {

        @Test
        @DisplayName("(1) alert exists for user - returns detail")
        void getAlertDetail_exists_returnsDetail() {
            UserEntity u = user();
            UserAlertEntity alert = alert(1L, AlertStatus.UNSEEN);
            when(userAlertRepository.findByIdAndUser(1L, u)).thenReturn(Optional.of(alert));

            AlertDetailResponse result = alertQueryService.getAlertDetail(1L, u);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("(2) alert not found for user - throws ResourceNotFoundException")
        void getAlertDetail_notFound_throwsResourceNotFoundException() {
            UserEntity u = user();
            when(userAlertRepository.findByIdAndUser(99L, u)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertQueryService.getAlertDetail(99L, u))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(3) alert belongs to different user - throws ResourceNotFoundException")
        void getAlertDetail_differentUser_throwsResourceNotFoundException() {
            UserEntity u = user();
            // findByIdAndUser returns empty because alert belongs to someone else
            when(userAlertRepository.findByIdAndUser(1L, u)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertQueryService.getAlertDetail(1L, u))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}


