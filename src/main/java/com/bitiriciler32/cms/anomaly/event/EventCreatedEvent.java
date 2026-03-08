package com.bitiriciler32.cms.anomaly.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Domain event published after an anomaly event is persisted and alerts are created.
 * Consumed by the Notification module to trigger push notifications.
 */
@Getter
@AllArgsConstructor
public class EventCreatedEvent {
    private final Long eventId;
}
