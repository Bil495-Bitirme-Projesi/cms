package com.bitiriciler32.cms.media.controller;

import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import com.bitiriciler32.cms.media.dto.DownloadUrlResponse;
import com.bitiriciler32.cms.media.service.ClipStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates presigned download URLs for client video playback.
 * Validates that the authenticated user actually owns the alert.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class ClipAccessController {

    private final ClipStorageService clipStorageService;
    private final UserAlertRepository userAlertRepository;
    private final UserRepository userRepository;

    @GetMapping("/{alertId}/clip-url")
    public ResponseEntity<DownloadUrlResponse> getDownloadUrl(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long alertId) {

        UserEntity user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        UserAlertEntity alert = userAlertRepository.findByIdAndUser(alertId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        String clipObjectKey = alert.getEvent().getClipObjectKey();
        if (clipObjectKey == null || clipObjectKey.isBlank()) {
            // Clip not ready yet
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }

        DownloadUrlResponse response = clipStorageService.generateDownloadUrl(clipObjectKey);
        return ResponseEntity.ok(response);
    }
}
