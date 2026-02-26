package com.bitiriciler32.cms.media.service;

import com.bitiriciler32.cms.media.dto.DownloadUrlResponse;
import com.bitiriciler32.cms.media.dto.UploadUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Manages references to video clips stored on MinIO.
 * Generates presigned upload and download URLs.
 */
@Service
@RequiredArgsConstructor
public class ClipStorageService {

    private final StorageClient storageClient;

    @Value("${minio.presign-expiry-seconds}")
    private long presignExpirySeconds;

    /**
     * Generate a presigned PUT URL for the AI subsystem to upload a clip.
     */
    public UploadUrlResponse generateUploadUrl(Long cameraId, Long eventId) {
        String objectKey = buildObjectKey(cameraId, eventId);
        String uploadUrl = storageClient.presignPut(objectKey, presignExpirySeconds);
        return new UploadUrlResponse(objectKey, uploadUrl, presignExpirySeconds);
    }

    /**
     * Generate a presigned GET URL for client playback.
     */
    public DownloadUrlResponse generateDownloadUrl(String objectKey) {
        String downloadUrl = storageClient.presignGet(objectKey, presignExpirySeconds);
        return new DownloadUrlResponse(downloadUrl, presignExpirySeconds);
    }

    /**
     * Build a deterministic object key for a clip: cameras/{cameraId}/events/{eventId}.mp4
     */
    private String buildObjectKey(Long cameraId, Long eventId) {
        return String.format("cameras/%d/events/%d.mp4", cameraId, eventId);
    }
}
