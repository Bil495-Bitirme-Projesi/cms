package com.bitiriciler32.cms.management.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain event published when a camera configuration changes.
 * Consumed by the WebSocket config sync layer to push delta updates to the AI node.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraConfigChangedEvent {
    private Long cameraId;
    /** UPSERT or DELETE */
    private String changeType;
}
