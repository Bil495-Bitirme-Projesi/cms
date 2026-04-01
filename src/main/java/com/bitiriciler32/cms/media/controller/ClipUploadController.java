package com.bitiriciler32.cms.media.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @deprecated This endpoint has been removed.
 * The presigned upload URL is now returned directly by POST /api/events/ingest.
 * Kept as a tombstone to return HTTP 410 Gone for any stale AIS implementations.
 */
@RestController
@RequestMapping("/api/clips")
public class ClipUploadController {

    @PostMapping("/upload-url")
    public ResponseEntity<Void> requestUploadUrl() {
        // Upload URL is now included in the POST /api/events/ingest response.
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
