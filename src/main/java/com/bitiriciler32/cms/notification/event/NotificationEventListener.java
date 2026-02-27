package com.bitiriciler32.cms.notification.event;

import com.bitiriciler32.cms.anomaly.event.EventCreatedEvent;
import com.bitiriciler32.cms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Listens for EventCreatedEvent signals from the Anomaly Event module
 * and triggers push notification delivery.
 *
 * Uses @TransactionalEventListener(AFTER_COMMIT) so that:
 *   - Notifications are only sent after DB data is committed and visible
 *   - Failures here do NOT roll back the anomaly event / alert records
 *
 * Uses @Async so that the HTTP response to the AI node is not delayed
 * by push notification delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCreated(EventCreatedEvent event) {
        try {
            log.info("Notification triggered for event id: {}", event.getEventId());
            notificationService.sendAlertNotifications(event.getEventId());
        } catch (Exception e) {
            // Log and swallow — DB records are already committed and safe.
            // A retry mechanism (e.g. scheduled job) can pick these up later.
            log.error("Failed to send notifications for event id: {}", event.getEventId(), e);
        }
    }
}
