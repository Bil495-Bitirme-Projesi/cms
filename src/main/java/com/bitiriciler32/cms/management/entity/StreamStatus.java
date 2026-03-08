package com.bitiriciler32.cms.management.entity;

/**
 * Represents the live stream connectivity status of a camera,
 * as reported by the AI Inference Subsystem.
 *
 * UNKNOWN  — Camera just registered; AI node has not reported yet.
 * ONLINE   — AI node is successfully receiving the RTSP stream.
 * OFFLINE  — AI node cannot reach the camera (network issue, camera powered off, etc.).
 */
public enum StreamStatus {
    UNKNOWN,
    ONLINE,
    OFFLINE
}

