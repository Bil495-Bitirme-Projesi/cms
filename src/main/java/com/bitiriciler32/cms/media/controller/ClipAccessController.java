package com.bitiriciler32.cms.media.controller;

import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.common.ApiErrorResponse;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import com.bitiriciler32.cms.media.dto.DownloadUrlResponse;
import com.bitiriciler32.cms.media.service.ClipStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates presigned download URLs for client video playback.
 * Validates that the authenticated user actually owns the alert.
 */
@Tag(name = "Clips", description = "Presigned download URL generation for anomaly video clips")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class ClipAccessController {

    private final ClipStorageService clipStorageService;
    private final UserAlertRepository userAlertRepository;
    private final UserRepository userRepository;

    @Operation(
            summary = "Get clip download URL",
            description = "Returns a time-limited presigned URL to stream or download the video clip "
                    + "associated with the given alert. The alert must belong to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presigned download URL generated",
                    content = @Content(schema = @Schema(implementation = DownloadUrlResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alert not found or does not belong to the authenticated user",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "userAuth")
    @GetMapping("/{alertId}/clip-url")
    public ResponseEntity<DownloadUrlResponse> getDownloadUrl(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long alertId) {

        UserEntity user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database"));

        UserAlertEntity alert = userAlertRepository.findByIdAndUser(alertId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        String clipObjectKey = alert.getEvent().getClipObjectKey();

        DownloadUrlResponse response = clipStorageService.generateDownloadUrl(clipObjectKey);
        return ResponseEntity.ok(response);
    }
}
