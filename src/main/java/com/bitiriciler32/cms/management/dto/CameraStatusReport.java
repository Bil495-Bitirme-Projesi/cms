package com.bitiriciler32.cms.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebSocket message sent by the AI Inference Subsystem to report
 * the connectivity status of a camera.
 *
 * Example payload:
 * { "type": "CAMERA_STATUS", "cameraId": 5, "status": "OFFLINE", "reportedAt": "2026-03-08T12:00:00Z" }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraStatusReport {
    private Long cameraId;
    /** "ONLINE" or "OFFLINE" */
    private String status;
    /** Timestamp from the AI node when it observed this status. */
    private Instant reportedAt;
}

