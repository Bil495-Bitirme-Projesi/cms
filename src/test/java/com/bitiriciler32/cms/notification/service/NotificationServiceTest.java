package com.bitiriciler32.cms.notification.service;

import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import com.bitiriciler32.cms.notification.entity.DeviceFcmTokenEntity;
import com.bitiriciler32.cms.notification.repository.DeviceFcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-024: NotificationService.sendAlertNotifications()
 * TC-CMS-025: NotificationService.sendCameraOfflineNotifications()
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock UserAlertRepository userAlertRepository;
    @Mock UserRepository userRepository;
    @Mock DeviceFcmTokenRepository deviceFcmTokenRepository;
    @Mock PushNotificationSender pushNotificationSender;

    @InjectMocks NotificationService notificationService;

    private UserEntity user(Long id) {
        return UserEntity.builder().id(id).name("U").email("u@t.com")
                .passwordHash("h").role(Role.OPERATOR).enabled(true).build();
    }

    private UserEntity admin(Long id) {
        return UserEntity.builder().id(id).name("Admin").email("a@t.com")
                .passwordHash("h").role(Role.ADMIN).enabled(true).build();
    }

    private CameraEntity camera() {
        return CameraEntity.builder().id(1L).name("Cam").rtspUrl("rtsp://x")
                .detectionEnabled(true).streamStatus(StreamStatus.UNKNOWN).deleted(false).build();
    }

    private AnomalyEventEntity event(Long id) {
        return AnomalyEventEntity.builder()
                .id(id).sourceEventId("s1").timestamp(Instant.now())
                .score(0.9).type("INTRUSION").description("desc").camera(camera()).build();
    }

    private UserAlertEntity alert(UserEntity user, AnomalyEventEntity event) {
        return UserAlertEntity.builder().id(1L).user(user).event(event).falsePositive(false).build();
    }

    private DeviceFcmTokenEntity token(UserEntity user, String fcm) {
        return DeviceFcmTokenEntity.builder().id(1L).user(user).fcmToken(fcm).enabled(true).build();
    }

    // ── TC-CMS-024: sendAlertNotifications() ────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-024: NotificationService.sendAlertNotifications()")
    class SendAlertNotificationsTests {

        @Test
        @DisplayName("(1) multiple alerts, users have tokens - sends notifications")
        void sendAlertNotifications_multipleAlertsWithTokens_sendsAll() {
            UserEntity u1 = user(1L);
            UserEntity u2 = user(2L);
            AnomalyEventEntity ev = event(10L);

            when(userAlertRepository.findByEventId(10L))
                    .thenReturn(List.of(alert(u1, ev), alert(u2, ev)));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(u1))
                    .thenReturn(List.of(token(u1, "token-1")));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(u2))
                    .thenReturn(List.of(token(u2, "token-2")));

            notificationService.sendAlertNotifications(10L);

            verify(pushNotificationSender).sendToToken(eq("token-1"), any(), any());
            verify(pushNotificationSender).sendToToken(eq("token-2"), any(), any());
        }

        @Test
        @DisplayName("(2) user has multiple tokens - sends to all tokens")
        void sendAlertNotifications_multipleTokensPerUser_sendsToAll() {
            UserEntity u = user(1L);
            AnomalyEventEntity ev = event(10L);

            when(userAlertRepository.findByEventId(10L)).thenReturn(List.of(alert(u, ev)));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(u))
                    .thenReturn(List.of(token(u, "tok-a"), token(u, "tok-b")));

            notificationService.sendAlertNotifications(10L);

            verify(pushNotificationSender).sendToToken(eq("tok-a"), any(), any());
            verify(pushNotificationSender).sendToToken(eq("tok-b"), any(), any());
        }

        @Test
        @DisplayName("(3) users have no tokens - no notifications sent")
        void sendAlertNotifications_noTokens_sendsNothing() {
            UserEntity u = user(1L);
            AnomalyEventEntity ev = event(10L);

            when(userAlertRepository.findByEventId(10L)).thenReturn(List.of(alert(u, ev)));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(u)).thenReturn(List.of());

            notificationService.sendAlertNotifications(10L);

            verify(pushNotificationSender, never()).sendToToken(any(), any(), any());
        }

        @Test
        @DisplayName("(4) event has no alerts - no notifications sent")
        void sendAlertNotifications_noAlerts_sendsNothing() {
            when(userAlertRepository.findByEventId(99L)).thenReturn(List.of());

            notificationService.sendAlertNotifications(99L);

            verify(pushNotificationSender, never()).sendToToken(any(), any(), any());
        }

        @Test
        @DisplayName("(5) sendToToken throws for one token - continues with others")
        void sendAlertNotifications_senderThrowsForOneToken_continuesWithOthers() {
            UserEntity u = user(1L);
            AnomalyEventEntity ev = event(10L);

            when(userAlertRepository.findByEventId(10L)).thenReturn(List.of(alert(u, ev)));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(u))
                    .thenReturn(List.of(token(u, "tok-a"), token(u, "tok-b")));

            // PushNotificationSender catches exceptions internally, so both tokens are attempted
            doNothing().when(pushNotificationSender).sendToToken(any(), any(), any());

            notificationService.sendAlertNotifications(10L);

            verify(pushNotificationSender, times(2)).sendToToken(any(), any(), any());
        }

        @Test
        @DisplayName("(6) DeviceFcmTokenRepository returns only enabled tokens")
        void sendAlertNotifications_repositoryReturnsOnlyEnabled() {
            UserEntity u = user(1L);
            AnomalyEventEntity ev = event(10L);

            when(userAlertRepository.findByEventId(10L)).thenReturn(List.of(alert(u, ev)));
            // Simulating that the repository filters only enabled tokens
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(u))
                    .thenReturn(List.of(token(u, "enabled-tok")));

            notificationService.sendAlertNotifications(10L);

            verify(pushNotificationSender, times(1)).sendToToken(eq("enabled-tok"), any(), any());
        }

        @Test
        @DisplayName("(7) DeviceFcmTokenRepository throws exception - propagates")
        void sendAlertNotifications_repositoryThrows_propagates() {
            UserEntity u = user(1L);
            AnomalyEventEntity ev = event(10L);

            when(userAlertRepository.findByEventId(10L)).thenReturn(List.of(alert(u, ev)));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(u))
                    .thenThrow(new RuntimeException("DB error"));

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> notificationService.sendAlertNotifications(10L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── TC-CMS-025: sendCameraOfflineNotifications() ─────────────────────────

    @Nested
    @DisplayName("TC-CMS-025: NotificationService.sendCameraOfflineNotifications()")
    class SendCameraOfflineNotificationsTests {

        @Test
        @DisplayName("(1) multiple enabled admins with tokens - sends to all")
        void sendCameraOffline_multipleAdmins_sendsToAll() {
            UserEntity a1 = admin(1L);
            UserEntity a2 = admin(2L);
            when(userRepository.findByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(List.of(a1, a2));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(a1))
                    .thenReturn(List.of(token(a1, "tok-a1")));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(a2))
                    .thenReturn(List.of(token(a2, "tok-a2")));

            notificationService.sendCameraOfflineNotifications(1L, "Front Door");

            verify(pushNotificationSender).sendToToken(eq("tok-a1"), any(), any());
            verify(pushNotificationSender).sendToToken(eq("tok-a2"), any(), any());
        }

        @Test
        @DisplayName("(2) single admin with multiple tokens - sends to all tokens")
        void sendCameraOffline_singleAdminMultipleTokens_sendsToAll() {
            UserEntity a = admin(1L);
            when(userRepository.findByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(List.of(a));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(a))
                    .thenReturn(List.of(token(a, "tok-1"), token(a, "tok-2")));

            notificationService.sendCameraOfflineNotifications(1L, "Cam");

            verify(pushNotificationSender, times(2)).sendToToken(any(), any(), any());
        }

        @Test
        @DisplayName("(3) no enabled admins - no notifications sent")
        void sendCameraOffline_noAdmins_sendsNothing() {
            when(userRepository.findByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(List.of());

            notificationService.sendCameraOfflineNotifications(1L, "Cam");

            verify(pushNotificationSender, never()).sendToToken(any(), any(), any());
        }

        @Test
        @DisplayName("(4) admins exist but have no tokens - no notifications sent")
        void sendCameraOffline_adminsWithoutTokens_sendsNothing() {
            UserEntity a = admin(1L);
            when(userRepository.findByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(List.of(a));
            when(deviceFcmTokenRepository.findByUserAndEnabledTrue(a)).thenReturn(List.of());

            notificationService.sendCameraOfflineNotifications(1L, "Cam");

            verify(pushNotificationSender, never()).sendToToken(any(), any(), any());
        }

        @Test
        @DisplayName("(5) UserRepository.findByRoleAndEnabledTrue() throws - propagates")
        void sendCameraOffline_repositoryThrows_propagates() {
            when(userRepository.findByRoleAndEnabledTrue(Role.ADMIN))
                    .thenThrow(new RuntimeException("DB error"));

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> notificationService.sendCameraOfflineNotifications(1L, "Cam"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

