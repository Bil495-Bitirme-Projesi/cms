package com.bitiriciler32.cms.management.event;

import lombok.Getter;

/**
 * Domain event published when a camera transitions to OFFLINE status.
 * Consumed by the Notification module to alert administrators.
 */
@Getter
public class CameraOfflineEvent {

    private final Long cameraId;
    private final String cameraName;

    public CameraOfflineEvent(Long cameraId, String cameraName) {
        this.cameraId = cameraId;
        this.cameraName = cameraName;
    }
}

