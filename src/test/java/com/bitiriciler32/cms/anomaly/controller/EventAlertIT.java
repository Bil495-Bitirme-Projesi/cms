package com.bitiriciler32.cms.anomaly.controller;

import com.bitiriciler32.cms.anomaly.entity.AlertStatus;
import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.AnomalyEventRepository;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import com.bitiriciler32.cms.support.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TC-CMS-017: POST /api/events/ingest
 * TC-CMS-019: GET /api/events (if exists) – alert-based queries via /api/alerts
 * TC-CMS-022: GET /api/alerts
 * TC-CMS-023: GET /api/alerts/{id}
 * TC-CMS-027: POST /api/device-tokens
 */
@DisplayName("Event Ingestion & Alert IT")
class EventAlertIT extends AbstractIT {

    @Autowired private CameraRepository cameraRepository;
    @Autowired private AnomalyEventRepository anomalyEventRepository;
    @Autowired private UserAlertRepository userAlertRepository;
    @Autowired private UserCameraAccessRepository userCameraAccessRepository;

    private CameraEntity testCamera;

    @BeforeEach
    void cleanUp() {
        userAlertRepository.deleteAll();
        anomalyEventRepository.deleteAll();
        userCameraAccessRepository.deleteAll();
        cameraRepository.deleteAll();

        testCamera = cameraRepository.save(CameraEntity.builder()
                .name("Test Camera")
                .rtspUrl("rtsp://test-cam")
                .detectionEnabled(true)
                .build());
    }

    // ── TC-CMS-017: POST /api/events/ingest ─────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-017: POST /api/events/ingest")
    class EventIngest {

        @Test
        @DisplayName("(1) Valid event via subsystem token → 201")
        void validEvent_returns201() throws Exception {
            // Grant operator access so alerts are created
            userCameraAccessRepository.save(UserCameraAccessEntity.builder()
                    .user(operatorUser).camera(testCamera).build());

            mockMvc.perform(post("/api/events/ingest")
                            .header("Authorization", "Bearer " + subsystemToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "sourceEventId": "evt-001",
                                        "cameraId": %d,
                                        "timestamp": "%s",
                                        "score": 0.95,
                                        "type": "INTRUSION",
                                        "description": "Test anomaly"
                                    }
                                    """.formatted(testCamera.getId(), Instant.now().toString())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("(3) Invalid cameraId → 404")
        void invalidCameraId_returns404() throws Exception {
            mockMvc.perform(post("/api/events/ingest")
                            .header("Authorization", "Bearer " + subsystemToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "sourceEventId": "evt-002",
                                        "cameraId": 99999,
                                        "timestamp": "%s",
                                        "score": 0.8,
                                        "type": "INTRUSION"
                                    }
                                    """.formatted(Instant.now().toString())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(4) Malformed request body → 400")
        void malformedBody_returns400() throws Exception {
            mockMvc.perform(post("/api/events/ingest")
                            .header("Authorization", "Bearer " + subsystemToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"sourceEventId":"","cameraId":null}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("(5) Unauthenticated request → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/events/ingest")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "sourceEventId": "evt-003",
                                        "cameraId": %d,
                                        "timestamp": "%s",
                                        "score": 0.8,
                                        "type": "INTRUSION"
                                    }
                                    """.formatted(testCamera.getId(), Instant.now().toString())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── TC-CMS-022: GET /api/alerts ─────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-022: GET /api/alerts")
    class GetAlerts {

        @Test
        @DisplayName("(1) User queries alerts → 200")
        void userQueriesAlerts_returns200() throws Exception {
            mockMvc.perform(get("/api/alerts")
                            .header("Authorization", "Bearer " + operatorToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("(3) OPERATOR queries alerts → returns only own alerts")
        void operatorQueriesOwnAlerts_returns200() throws Exception {
            // Create event and alert for operator
            AnomalyEventEntity event = anomalyEventRepository.save(AnomalyEventEntity.builder()
                    .sourceEventId("alert-evt-1")
                    .camera(testCamera)
                    .timestamp(Instant.now())
                    .score(0.9)
                    .type("INTRUSION")
                    .build());

            userAlertRepository.save(UserAlertEntity.builder()
                    .user(operatorUser)
                    .event(event)
                    .status(AlertStatus.UNSEEN)
                    .build());

            mockMvc.perform(get("/api/alerts")
                            .header("Authorization", "Bearer " + operatorToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // ── TC-CMS-023: GET /api/alerts/{id} ────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-023: GET /api/alerts/{id}")
    class GetAlertDetail {

        @Test
        @DisplayName("(1) Authorized user gets alert detail → 200")
        void authorizedUser_returns200() throws Exception {
            AnomalyEventEntity event = anomalyEventRepository.save(AnomalyEventEntity.builder()
                    .sourceEventId("detail-evt-1")
                    .camera(testCamera)
                    .timestamp(Instant.now())
                    .score(0.85)
                    .type("LOITERING")
                    .build());

            UserAlertEntity alert = userAlertRepository.save(UserAlertEntity.builder()
                    .user(operatorUser)
                    .event(event)
                    .status(AlertStatus.UNSEEN)
                    .build());

            mockMvc.perform(get("/api/alerts/" + alert.getId())
                            .header("Authorization", "Bearer " + operatorToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(alert.getId()));
        }

        @Test
        @DisplayName("(3) Alert not found → 404")
        void alertNotFound_returns404() throws Exception {
            mockMvc.perform(get("/api/alerts/99999")
                            .header("Authorization", "Bearer " + operatorToken()))
                    .andExpect(status().isNotFound());
        }
    }
}

