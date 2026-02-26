package com.bitiriciler32.cms.anomaly.controller;

import com.bitiriciler32.cms.anomaly.dto.EventIngestRequest;
import com.bitiriciler32.cms.anomaly.dto.EventIngestResponse;
import com.bitiriciler32.cms.anomaly.service.EventIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives anomaly event metadata from the AI Inference Subsystem.
 * Authenticated via subsystem JWT.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventIngestController {

    private final EventIngestService eventIngestService;

    @PostMapping("/ingest")
    public ResponseEntity<EventIngestResponse> ingest(
            @Valid @RequestBody EventIngestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventIngestService.ingest(request));
    }
}
