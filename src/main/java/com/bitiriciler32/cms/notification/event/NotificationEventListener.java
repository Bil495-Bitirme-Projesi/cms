package com.bitiriciler32.cms.notification.event;

import com.bitiriciler32.cms.anomaly.event.EventCreatedEvent;
import com.bitiriciler32.cms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for EventCreatedEvent signals from the Anomaly Event module
 * and triggers push notification delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void onEventCreated(EventCreatedEvent event) {
        log.info("Notification triggered for event id: {}", event.getEventId());
        notificationService.sendAlertNotifications(event.getEventId());
    }
}
