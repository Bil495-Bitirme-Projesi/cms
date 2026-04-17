package com.bitiriciler32.cms.notification.controller;

import com.bitiriciler32.cms.notification.repository.DeviceFcmTokenRepository;
import com.bitiriciler32.cms.support.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-CMS-027: POST /api/device-tokens
 */
@DisplayName("Device Token IT")
class DeviceTokenControllerIT extends AbstractIT {

    @Autowired
    private DeviceFcmTokenRepository deviceFcmTokenRepository;

    @BeforeEach
    void cleanTokens() {
        deviceFcmTokenRepository.deleteAll();
    }

    @Nested
    @DisplayName("TC-CMS-027: POST /api/device-tokens")
    class RegisterDevice {

        @Test
        @DisplayName("(1) User registers new device token → 201")
        void registerNewToken_returns201() throws Exception {
            mockMvc.perform(post("/api/device-tokens")
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fcmToken":"fake-fcm-token-12345"}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("(2) User registers duplicate token → replaces existing, 201")
        void registerDuplicateToken_returns201() throws Exception {
            // First registration
            mockMvc.perform(post("/api/device-tokens")
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fcmToken":"token-v1"}
                                    """))
                    .andExpect(status().isCreated());

            // Second registration (replaces)
            mockMvc.perform(post("/api/device-tokens")
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fcmToken":"token-v2"}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("(4) Invalid token format (blank) → 400")
        void blankToken_returns400() throws Exception {
            mockMvc.perform(post("/api/device-tokens")
                            .header("Authorization", "Bearer " + operatorToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fcmToken":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("(5) Unauthenticated request → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/device-tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"fcmToken":"some-token"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }
}

