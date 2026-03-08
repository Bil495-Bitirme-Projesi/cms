package com.bitiriciler32.cms.notification.event;

import com.bitiriciler32.cms.anomaly.event.EventCreatedEvent;
import com.bitiriciler32.cms.management.event.CameraOfflineEvent;
import com.bitiriciler32.cms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Central listener for all notification-triggering domain events.
 *
 * onEventCreated: anomaly alert push notifications to assigned operators.
 * onCameraOffline: camera-down push notifications to admin users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Anomaly event → push notifications to assigned operators.
     *
     * @TransactionalEventListener(AFTER_COMMIT): only fires after anomaly data is committed.
     * @Async: HTTP response to AI node is not delayed by FCM calls.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCreated(EventCreatedEvent event) {
        try {
            log.info("Notification triggered for event id: {}", event.getEventId());
            notificationService.sendAlertNotifications(event.getEventId());
        } catch (Exception e) {
            log.error("Failed to send notifications for event id: {}", event.getEventId(), e);
        }
    }

    /**
     * Camera went offline → push notifications to admin users.
     *
     * @TransactionalEventListener(AFTER_COMMIT): the event is published inside
     * CameraHealthService.applyStatusReport()'s @Transactional method.
     * We must wait for that transaction to commit so that:
     *   - streamStatus and lastOfflineNotifiedAt are persisted before notification fires
     *   - if the transaction rolls back, no spurious notification is sent
     *
     * @Async: WebSocket message processing thread is not blocked by FCM calls.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCameraOffline(CameraOfflineEvent event) {
        try {
            log.info("Camera offline notification triggered: cameraId={}, name={}",
                    event.getCameraId(), event.getCameraName());
            notificationService.sendCameraOfflineNotifications(event.getCameraId(), event.getCameraName());
        } catch (Exception e) {
            log.error("Failed to send camera-offline notifications for cameraId={}: {}",
                    event.getCameraId(), e.getMessage(), e);
        }
    }
}
