package com.bitiriciler32.cms.notification.service;

import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.notification.entity.DeviceFcmTokenEntity;
import com.bitiriciler32.cms.notification.repository.DeviceFcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserAlertRepository userAlertRepository;
    private final DeviceFcmTokenRepository deviceFcmTokenRepository;
    private final PushNotificationSender pushNotificationSender;

    /**
     * Send push notifications to all users who have alerts for the given event.
     */
    public void sendAlertNotifications(Long eventId) {
        List<UserAlertEntity> alerts = userAlertRepository.findByEventId(eventId);

        for (UserAlertEntity alert : alerts) {
            UserEntity user = alert.getUser();
            AnomalyEventEntity event = alert.getEvent();

            List<DeviceFcmTokenEntity> tokens = deviceFcmTokenRepository.findByUserAndEnabledTrue(user);

            String title = "Anomaly Detected — " + event.getSeverity();
            String body = String.format("Camera %d: %s (score %.2f)",
                    event.getCamera().getId(), event.getType(), event.getScore());

            for (DeviceFcmTokenEntity deviceToken : tokens) {
                pushNotificationSender.sendToToken(deviceToken.getFcmToken(), title, body);
            }

            log.debug("Sent {} push notifications to user {} for event {}",
                    tokens.size(), user.getId(), eventId);
        }
    }
}
