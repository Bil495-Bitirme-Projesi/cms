package com.bitiriciler32.cms.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends push notifications via Firebase Cloud Messaging.
 */
@Component
@Slf4j
public class PushNotificationSender {

    public void sendToToken(String token, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Skipping push notification to token: {}", token);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.debug("Push notification sent successfully: {}", response);
        } catch (Exception e) {
            // Full stack trace intentionally logged here to diagnose credential / network issues
            log.error("Failed to send push notification to token {}", token, e);
        }
    }
}
