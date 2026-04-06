package com.bitiriciler32.cms.notification.controller;

import com.bitiriciler32.cms.common.ApiErrorResponse;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import com.bitiriciler32.cms.notification.dto.RegisterTokenRequest;
import com.bitiriciler32.cms.notification.entity.DeviceFcmTokenEntity;
import com.bitiriciler32.cms.notification.repository.DeviceFcmTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Manages FCM device tokens for push notifications.
 * Each user is allowed at most one active token at a time, matching the
 * single-session guarantee enforced by the tokenVersion mechanism in the JWT layer.
 */
@Tag(name = "Device Tokens", description = "FCM push-notification token registration – authenticated users")
@SecurityRequirement(name = "userAuth")
@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceFcmTokenRepository deviceFcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * Register (or replace) the FCM token for the currently authenticated user.
     * Any previously stored tokens for this user are deleted before saving the new one,
     * so there is always at most one active token per user in the database.
     * <p>
     * This endpoint should be called immediately after a successful login, once the
     * mobile app has obtained the FCM token from the Firebase SDK.
     */
    @Operation(
            summary = "Register FCM device token",
            description = "Stores the FCM token for the authenticated user. "
                    + "Any existing token for this user is replaced, keeping exactly one active entry per user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Token registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error – missing or blank token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @Transactional
    @PostMapping
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegisterTokenRequest request) {

        UserEntity user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database"));

        // Replace semantics: remove any existing token before saving the new one.
        // This guarantees at most one active FCM token entry per user, consistent with
        // the single-session policy enforced at the JWT level.
        deviceFcmTokenRepository.deleteByUser(user);

        DeviceFcmTokenEntity token = DeviceFcmTokenEntity.builder()
                .user(user)
                .fcmToken(request.getFcmToken())
                .enabled(true)
                .build();

        deviceFcmTokenRepository.save(token);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Delete the FCM token for the currently authenticated user.
     * Should be called during logout, before the JWT is discarded on the client side,
     * to prevent push notifications from being sent to a logged-out device.
     */
    @Operation(
            summary = "Unregister FCM device token",
            description = "Deletes the FCM token for the authenticated user. "
                    + "Call this endpoint as part of the logout flow, before discarding the JWT on the client, "
                    + "to stop push notifications from being delivered to a logged-out device.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token unregistered (or no token was registered)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @Transactional
    @DeleteMapping
    public ResponseEntity<Void> unregisterToken(@AuthenticationPrincipal UserDetails userDetails) {

        UserEntity user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database"));

        deviceFcmTokenRepository.deleteByUser(user);
        return ResponseEntity.noContent().build();
    }
}
