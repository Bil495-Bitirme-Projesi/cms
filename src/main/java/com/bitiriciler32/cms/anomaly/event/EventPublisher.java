package com.bitiriciler32.cms.anomaly.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * <<flow-boundary>> — publishes event-created signals.
 * Transfers responsibility to the Notification module for push delivery.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishEventCreated(Long eventId) {
        applicationEventPublisher.publishEvent(new EventCreatedEvent(this, eventId));
    }
}
