package com.bitiriciler32.cms.anomaly.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published after an anomaly event is persisted and alerts are created.
 * Consumed by the Notification module to trigger push notifications.
 */
@Getter
public class EventCreatedEvent extends ApplicationEvent {

    private final Long eventId;

    public EventCreatedEvent(Object source, Long eventId) {
        super(source);
        this.eventId = eventId;
    }
}
