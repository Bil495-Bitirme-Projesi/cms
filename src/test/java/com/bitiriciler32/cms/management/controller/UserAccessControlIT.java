package com.bitiriciler32.cms.management.controller;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import com.bitiriciler32.cms.support.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TC-CMS-040: POST /api/admin/users
 * TC-CMS-041: PUT /api/admin/users/{id}
 * TC-CMS-042: DELETE /api/admin/users/{id}
 * TC-CMS-043: POST /api/admin/access/grant
 * TC-CMS-044: DELETE /api/admin/access/revoke
 * TC-CMS-045: GET /api/admin/access/{userId}
 */
@DisplayName("User & Access Control Management IT")
class UserAccessControlIT extends AbstractIT {

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private UserCameraAccessRepository userCameraAccessRepository;

    @BeforeEach
    void cleanUp() {
        userCameraAccessRepository.deleteAll();
        cameraRepository.deleteAll();
    }

    // ── TC-CMS-040: POST /api/admin/users ───────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-040: POST /api/admin/users")
    class CreateUser {

        @Test
        @DisplayName("(1) ADMIN creates user with valid data → 201")
        void adminCreatesUser_returns201() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"New User","email":"new@test.com","password":"password123","role":"OPERATOR","enabled":true}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("New User"));
        }

        @Test
        @DisplayName("(2) Duplicate email → 409")
        void duplicateEmail_returns409() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Dup","email":"admin@test.com","password":"password123","role":"OPERATOR"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("(3) OPERATOR attempts to create user → 403")
        void operatorCreatesUser_returns403() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"X","email":"x@test.com","password":"password123","role":"OPERATOR"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("(4) Invalid request body → 400")
        void invalidBody_returns400() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"","email":"invalid","password":"short","role":"OPERATOR"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── TC-CMS-041: PUT /api/admin/users/{id} ───────────────────────────────

    @Nested
    @DisplayName("TC-CMS-041: PUT /api/admin/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("(1) ADMIN updates existing user → 200")
        void adminUpdatesUser_returns200() throws Exception {
            mockMvc.perform(put("/api/admin/users/" + operatorUser.getId())
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Updated Operator"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Operator"));
        }

        @Test
        @DisplayName("(2) Update non-existent user → 404")
        void updateNonExistent_returns404() throws Exception {
            mockMvc.perform(put("/api/admin/users/99999")
                            .header("Authorization", "Bearer " + adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Ghost"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(3) OPERATOR attempts to update user → 403")
        void operatorUpdatesUser_returns403() throws Exception {
            mockMvc.perform(put("/api/admin/users/" + operatorUser.getId())
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Hacked"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    // ── TC-CMS-042: DELETE /api/admin/users/{id} ────────────────────────────

    @Nested
    @DisplayName("TC-CMS-042: DELETE /api/admin/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("(1) ADMIN deletes user → 204")
        void adminDeletesUser_returns204() throws Exception {
            // Create a disposable user
            UserEntity disposable = userRepository.save(UserEntity.builder()
                    .name("Disposable").email("disposable@test.com")
                    .passwordHash(passwordEncoder.encode("pass12345"))
                    .role(Role.OPERATOR).enabled(true).tokenVersion(1L).build());

            mockMvc.perform(delete("/api/admin/users/" + disposable.getId())
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("(2) Delete non-existent user → 404")
        void deleteNonExistent_returns404() throws Exception {
            mockMvc.perform(delete("/api/admin/users/99999")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(3) OPERATOR attempts to delete user → 403")
        void operatorDeletesUser_returns403() throws Exception {
            mockMvc.perform(delete("/api/admin/users/" + operatorUser.getId())
                            .header("Authorization", "Bearer " + operatorToken()))
                    .andExpect(status().isForbidden());
        }
    }

    // ── TC-CMS-043: POST /api/admin/access/grant ────────────────────────────

    @Nested
    @DisplayName("TC-CMS-043: POST /api/admin/access/grant")
    class GrantAccess {

        @Test
        @DisplayName("(1) ADMIN grants access to OPERATOR → 201")
        void adminGrantsAccess_returns201() throws Exception {
            CameraEntity cam = cameraRepository.save(CameraEntity.builder()
                    .name("Cam1").rtspUrl("rtsp://cam1").detectionEnabled(true).build());

            mockMvc.perform(post("/api/admin/access/grant")
                            .header("Authorization", "Bearer " + adminToken())
                            .param("userId", operatorUser.getId().toString())
                            .param("cameraId", cam.getId().toString()))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("(2) Grant access with invalid userId → 404")
        void invalidUserId_returns404() throws Exception {
            CameraEntity cam = cameraRepository.save(CameraEntity.builder()
                    .name("Cam2").rtspUrl("rtsp://cam2").detectionEnabled(true).build());

            mockMvc.perform(post("/api/admin/access/grant")
                            .header("Authorization", "Bearer " + adminToken())
                            .param("userId", "99999")
                            .param("cameraId", cam.getId().toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(3) Grant access with invalid cameraId → 404")
        void invalidCameraId_returns404() throws Exception {
            mockMvc.perform(post("/api/admin/access/grant")
                            .header("Authorization", "Bearer " + adminToken())
                            .param("userId", operatorUser.getId().toString())
                            .param("cameraId", "99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(4) Duplicate access grant → 409")
        void duplicateGrant_returns409() throws Exception {
            CameraEntity cam = cameraRepository.save(CameraEntity.builder()
                    .name("Cam3").rtspUrl("rtsp://cam3").detectionEnabled(true).build());

            userCameraAccessRepository.save(UserCameraAccessEntity.builder()
                    .user(operatorUser).camera(cam).build());

            mockMvc.perform(post("/api/admin/access/grant")
                            .header("Authorization", "Bearer " + adminToken())
                            .param("userId", operatorUser.getId().toString())
                            .param("cameraId", cam.getId().toString()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("(5) OPERATOR attempts to grant access → 403")
        void operatorGrantsAccess_returns403() throws Exception {
            CameraEntity cam = cameraRepository.save(CameraEntity.builder()
                    .name("Cam4").rtspUrl("rtsp://cam4").detectionEnabled(true).build());

            mockMvc.perform(post("/api/admin/access/grant")
                            .header("Authorization", "Bearer " + operatorToken())
                            .param("userId", operatorUser.getId().toString())
                            .param("cameraId", cam.getId().toString()))
                    .andExpect(status().isForbidden());
        }
    }

    // ── TC-CMS-044: DELETE /api/admin/access/revoke ─────────────────────────

    @Nested
    @DisplayName("TC-CMS-044: DELETE /api/admin/access/revoke")
    class RevokeAccess {

        @Test
        @DisplayName("(1) ADMIN revokes access → 204")
        void adminRevokesAccess_returns204() throws Exception {
            CameraEntity cam = cameraRepository.save(CameraEntity.builder()
                    .name("Cam5").rtspUrl("rtsp://cam5").detectionEnabled(true).build());
            userCameraAccessRepository.save(UserCameraAccessEntity.builder()
                    .user(operatorUser).camera(cam).build());

            mockMvc.perform(delete("/api/admin/access/revoke")
                            .header("Authorization", "Bearer " + adminToken())
                            .param("userId", operatorUser.getId().toString())
                            .param("cameraId", cam.getId().toString()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("(2) Revoke non-existent access → 404")
        void revokeNonExistent_returns404() throws Exception {
            CameraEntity cam = cameraRepository.save(CameraEntity.builder()
                    .name("Cam6").rtspUrl("rtsp://cam6").detectionEnabled(true).build());

            mockMvc.perform(delete("/api/admin/access/revoke")
                            .header("Authorization", "Bearer " + adminToken())
                            .param("userId", operatorUser.getId().toString())
                            .param("cameraId", cam.getId().toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("(3) OPERATOR attempts to revoke access → 403")
        void operatorRevokesAccess_returns403() throws Exception {
            mockMvc.perform(delete("/api/admin/access/revoke")
                            .header("Authorization", "Bearer " + operatorToken())
                            .param("userId", operatorUser.getId().toString())
                            .param("cameraId", "1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── TC-CMS-045: GET /api/admin/access/{userId} ──────────────────────────

    @Nested
    @DisplayName("TC-CMS-045: GET /api/admin/access/{userId}")
    class GetAccessList {

        @Test
        @DisplayName("(1) ADMIN queries access list for user → 200")
        void adminQueriesAccessList_returns200() throws Exception {
            CameraEntity cam = cameraRepository.save(CameraEntity.builder()
                    .name("Cam7").rtspUrl("rtsp://cam7").detectionEnabled(true).build());
            userCameraAccessRepository.save(UserCameraAccessEntity.builder()
                    .user(operatorUser).camera(cam).build());

            mockMvc.perform(get("/api/admin/access/" + operatorUser.getId())
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}

