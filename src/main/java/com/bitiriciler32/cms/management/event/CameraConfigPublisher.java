package com.bitiriciler32.cms.management.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * <<flow-boundary>> — publishes camera config changes as Spring ApplicationEvents.
 * This transfers responsibility to the WebSocket config sync layer.
 */
@Component
@RequiredArgsConstructor
public class CameraConfigPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishUpsert(Long cameraId) {
        eventPublisher.publishEvent(new CameraConfigChangedEvent(cameraId, "UPSERT"));
    }

    public void publishDelete(Long cameraId) {
        eventPublisher.publishEvent(new CameraConfigChangedEvent(cameraId, "DELETE"));
    }
}
