package com.bitiriciler32.cms.management.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 *
 * The "type" field is used for routing in InferenceWsEndpoint and is not mapped here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CameraStatusReport {
    private Long cameraId;
    /** "ONLINE" or "OFFLINE" */
    private String status;
    /** Timestamp from the AI node when it observed this status. */
    private Instant reportedAt;
}

