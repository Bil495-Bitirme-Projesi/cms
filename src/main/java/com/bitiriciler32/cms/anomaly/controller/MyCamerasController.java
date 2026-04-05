package com.bitiriciler32.cms.anomaly.controller;

import com.bitiriciler32.cms.anomaly.dto.CameraInfo;
import com.bitiriciler32.cms.common.ApiErrorResponse;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import com.bitiriciler32.cms.management.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Returns the cameras explicitly assigned to the authenticated user.
 *
 * Both ADMIN and OPERATOR roles use the same UserCameraAccess entries.
 * Admins who need the full camera list should use GET /api/admin/cameras.
 *
 * Primary use-case: populate the camera filter dropdown in the alert list screen.
 */
@Tag(name = "Cameras (user)", description = "Camera access queries for the authenticated user")
@SecurityRequirement(name = "userAuth")
@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
public class MyCamerasController {

    private final UserRepository userRepository;
    private final UserCameraAccessRepository userCameraAccessRepository;

    @Operation(
            summary = "Get my cameras",
            description = """
                    Returns the cameras explicitly assigned to the authenticated user
                    via the access-control configuration.
                    Use the returned IDs to populate the `cameraId` filter in `GET /api/alerts`.
                    Admins who need the full camera list should use `GET /api/admin/cameras`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camera list returned (may be empty)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CameraInfo.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/my")
    public ResponseEntity<List<CameraInfo>> getMyCameras(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserEntity user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + userDetails.getUsername()));

        List<CameraInfo> cameras = userCameraAccessRepository.findByUser(user).stream()
                .map(a -> new CameraInfo(a.getCamera().getId(), a.getCamera().getName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(cameras);
    }
}


