package com.bitiriciler32.cms.management.controller;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.support.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TC-CMS-010: POST /api/admin/cameras
 * TC-CMS-011: PUT /api/admin/cameras/{id}
 * TC-CMS-012: DELETE /api/admin/cameras/{id}
 * TC-CMS-013: GET /api/admin/cameras and GET /api/admin/cameras/{id}
 */
@DisplayName("Camera Management IT")
class CameraManagementControllerIT extends AbstractIT {

    @Autowired
    private CameraRepository cameraRepository;

    @BeforeEach
    void cleanCameras() {
        cameraRepository.deleteAll();
    }

    private CameraEntity createTestCamera(String name, String rtspUrl) {
        return cameraRepository.save(CameraEntity.builder()
                .name(name)
                .rtspUrl(rtspUrl)
                .detectionEnabled(true)
                .build());
    }

    // ── TC-CMS-010: POST /api/admin/cameras ─────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-010: POST /api/admin/cameras")
    class CreateCamera {

        @Test
        @DisplayName("(1) ADMIN creates camera with valid data → 201")
        void adminCreatesCamera_returns201() throws Exception {
            mockMvc.perform(post("/api/admin/cameras")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Lobby Cam","rtspUrl":"rtsp://192.168.1.10/stream1","detectionEnabled":true}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Lobby Cam"));
        }

        @Test
        @DisplayName("(3) OPERATOR attempts to create camera → 403")
        void operatorCreatesCamera_returns403() throws Exception {
            mockMvc.perform(post("/api/admin/cameras")
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Lobby Cam","rtspUrl":"rtsp://192.168.1.10/stream1"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("(4) Invalid request body → 400")
        void invalidRequestBody_returns400() throws Exception {
            mockMvc.perform(post("/api/admin/cameras")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"","rtspUrl":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── TC-CMS-011: PUT /api/admin/cameras/{id} ─────────────────────────────

    @Nested
    @DisplayName("TC-CMS-011: PUT /api/admin/cameras/{id}")
    class UpdateCamera {

        @Test
        @DisplayName("(1) ADMIN updates existing camera → 200")
        void adminUpdatesCamera_returns200() throws Exception {
            CameraEntity cam = createTestCamera("Old Name", "rtsp://old");

            mockMvc.perform(put("/api/admin/cameras/" + cam.getId())
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"New Name"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"));
        }

        @Test
        @DisplayName("(2) Update non-existent camera → 404")
        void updateNonExistent_returns404() throws Exception {
            mockMvc.perform(put("/api/admin/cameras/99999")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"New Name"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(3) OPERATOR attempts to update camera → 403")
        void operatorUpdatesCamera_returns403() throws Exception {
            CameraEntity cam = createTestCamera("Cam", "rtsp://cam");

            mockMvc.perform(put("/api/admin/cameras/" + cam.getId())
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Hacked"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    // ── TC-CMS-012: DELETE /api/admin/cameras/{id} ──────────────────────────

    @Nested
    @DisplayName("TC-CMS-012: DELETE /api/admin/cameras/{id}")
    class DeleteCamera {

        @Test
        @DisplayName("(1) ADMIN deletes existing camera → 204")
        void adminDeletesCamera_returns204() throws Exception {
            CameraEntity cam = createTestCamera("ToDelete", "rtsp://del");

            mockMvc.perform(delete("/api/admin/cameras/" + cam.getId())
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("(2) Delete non-existent camera → 404")
        void deleteNonExistent_returns404() throws Exception {
            mockMvc.perform(delete("/api/admin/cameras/99999")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isNotFound());
        }
    }

    // ── TC-CMS-013: GET /api/admin/cameras ──────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-013: GET /api/admin/cameras")
    class GetCameras {

        @Test
        @DisplayName("(1) Get all cameras as ADMIN → returns list")
        void getAllAsAdmin_returnsList() throws Exception {
            createTestCamera("Cam1", "rtsp://cam1");
            createTestCamera("Cam2", "rtsp://cam2");

            mockMvc.perform(get("/api/admin/cameras")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("(3) Get camera by valid ID → 200")
        void getByValidId_returns200() throws Exception {
            CameraEntity cam = createTestCamera("Single", "rtsp://single");

            mockMvc.perform(get("/api/admin/cameras/" + cam.getId())
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Single"));
        }

        @Test
        @DisplayName("(4) Get camera by invalid ID → 404")
        void getByInvalidId_returns404() throws Exception {
            mockMvc.perform(get("/api/admin/cameras/99999")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(5) Unauthorized user accessing cameras → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/admin/cameras"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

