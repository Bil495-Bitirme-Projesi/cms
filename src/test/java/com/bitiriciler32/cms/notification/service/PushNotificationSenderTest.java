package com.bitiriciler32.cms.notification.service;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TC-CMS-026: PushNotificationSender.sendToToken()
 *
 * Note: FirebaseMessaging cannot be easily unit-tested without a real Firebase App.
 * Scenarios that depend on FirebaseMessaging.send() are tested via behaviour verification
 * at the NotificationService level (TC-CMS-024/025) or in integration tests.
 * Here we focus on scenarios that can be verified without a live Firebase App.
 */
@DisplayName("TC-CMS-026: PushNotificationSender.sendToToken()")
class PushNotificationSenderTest {

    private final PushNotificationSender sender = new PushNotificationSender();

    @AfterEach
    void cleanupFirebase() {
        // Ensure Firebase is not initialised between test scenarios
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
    }

    @Nested
    @DisplayName("Firebase not initialised")
    class FirebaseNotInitialisedTests {

        @Test
        @DisplayName("(2) Firebase not initialized - logs warning and returns early (no exception)")
        void sendToToken_firebaseNotInitialized_returnsEarly() {
            // Firebase.getApps() is empty — no FirebaseApp has been initialised
            assertThatCode(() -> sender.sendToToken("any-token", "Title", "Body"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("(5) null token - no exception (Firebase not initialized, returns early)")
        void sendToToken_nullToken_noException() {
            assertThatCode(() -> sender.sendToToken(null, "Title", "Body"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("(6) null title and body - no exception (Firebase not initialized, returns early)")
        void sendToToken_nullTitleAndBody_noException() {
            assertThatCode(() -> sender.sendToToken("token", null, null))
                    .doesNotThrowAnyException();
        }
    }
}

