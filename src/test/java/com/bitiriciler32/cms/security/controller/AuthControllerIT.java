package com.bitiriciler32.cms.security.controller;

import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.support.AbstractIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TC-CMS-001: POST /api/auth/login
 * TC-CMS-002: POST /api/auth/subsystem-login
 * TC-CMS-003: Role-based access control
 */
@DisplayName("Auth Controller IT")
class AuthControllerIT extends AbstractIT {

    // ── TC-CMS-001: User Login ──────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-001: POST /api/auth/login")
    class UserLogin {

        @Test
        @DisplayName("(1) Valid credentials → 200 + JWT token")
        void validCredentials_returns200WithToken() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"admin@test.com","password":"admin-pass"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("(2) Invalid password → 401")
        void invalidPassword_returns401() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"admin@test.com","password":"wrong-pass"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("(3) Non-existent user → 401")
        void nonExistentUser_returns401() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"nobody@test.com","password":"whatever"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("(4) Disabled user → 401")
        void disabledUser_returns401() throws Exception {
            // Create a disabled user
            userRepository.save(UserEntity.builder()
                    .name("Disabled")
                    .email("disabled@test.com")
                    .passwordHash(passwordEncoder.encode("disabled-pass"))
                    .role(Role.OPERATOR)
                    .enabled(false)
                    .tokenVersion(1L)
                    .build());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"disabled@test.com","password":"disabled-pass"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("(5) Malformed request body → 400")
        void malformedBody_returns400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"","password":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── TC-CMS-002: Subsystem Login ─────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-002: POST /api/auth/subsystem-login")
    class SubsystemLogin {

        @Test
        @DisplayName("(1) Valid subsystem credentials → 200 + JWT")
        void validSubsystemCredentials_returns200() throws Exception {
            mockMvc.perform(post("/api/auth/subsystem-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"subsystemId":"test-subsystem","subsystemSecret":"test-subsystem-secret"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("(2) Invalid subsystem secret → 401")
        void invalidSecret_returns401() throws Exception {
            mockMvc.perform(post("/api/auth/subsystem-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"subsystemId":"test-subsystem","subsystemSecret":"wrong-secret"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("(3) Non-existent subsystem ID → 401")
        void nonExistentSubsystem_returns401() throws Exception {
            mockMvc.perform(post("/api/auth/subsystem-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"subsystemId":"unknown","subsystemSecret":"whatever"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── TC-CMS-003: Role-based access control ───────────────────────────────

    @Nested
    @DisplayName("TC-CMS-003: Role-based access control")
    class RoleBasedAccessControl {

        @Test
        @DisplayName("(1) ADMIN accessing admin endpoint → 200")
        void adminAccessingAdminEndpoint_returns200() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/admin/cameras")
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("(2) OPERATOR accessing admin endpoint → 403")
        void operatorAccessingAdminEndpoint_returns403() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/admin/cameras")
                            .header("Authorization", "Bearer " + operatorToken()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("(3) OPERATOR accessing operator endpoint → 200")
        void operatorAccessingOperatorEndpoint_returns200() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/alerts")
                            .header("Authorization", "Bearer " + operatorToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("(4) Unauthenticated request → 401")
        void unauthenticatedRequest_returns401() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/admin/cameras"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

