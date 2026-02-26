package com.bitiriciler32.cms.media.controller;

import com.bitiriciler32.cms.media.dto.UploadUrlRequest;
import com.bitiriciler32.cms.media.dto.UploadUrlResponse;
import com.bitiriciler32.cms.media.service.ClipStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates presigned upload URLs for the AI Inference Subsystem.
 * Authenticated via subsystem JWT.
 */
@RestController
@RequestMapping("/api/clips")
@RequiredArgsConstructor
public class ClipUploadController {

    private final ClipStorageService clipStorageService;

    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> requestUploadUrl(
            @RequestBody UploadUrlRequest request) {
        return ResponseEntity.ok(
                clipStorageService.generateUploadUrl(request.getCameraId(), request.getEventId()));
    }
}
