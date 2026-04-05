package com.bitiriciler32.cms.management.controller;

import com.bitiriciler32.cms.common.ApiErrorResponse;
import com.bitiriciler32.cms.management.dto.UserCameraAccessResponse;
import com.bitiriciler32.cms.management.service.AccessControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Access Control", description = "User–camera access management – ADMIN only")
@SecurityRequirement(name = "userAuth")
@RestController
@RequestMapping("/api/admin/access")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccessControlManagementController {

    private final AccessControlService accessControlService;

    @Operation(summary = "Grant camera access",
            description = "Grants a user access to a specific camera. "
                    + "Once granted, the user will receive alert notifications for anomalies detected by that camera. "
                    + "Returns 400 if the target user has the ADMIN role "
                    + "(ADMIN users are not subject to per-camera access assignments).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Access granted"),
            @ApiResponse(responseCode = "400", description = "Target user is an ADMIN – access assignments do not apply",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or camera not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The user already has access to this camera",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/grant")
    public ResponseEntity<Void> grantAccess(@RequestParam Long userId,
                                             @RequestParam Long cameraId) {
        accessControlService.grantAccess(userId, cameraId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Revoke camera access",
            description = "Removes a user's access to a specific camera. "
                    + "The user will no longer receive alert notifications for that camera.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Access revoked"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User, camera, or access mapping not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/revoke")
    public ResponseEntity<Void> revokeAccess(@RequestParam Long userId,
                                              @RequestParam Long cameraId) {
        accessControlService.revokeAccess(userId, cameraId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get camera access list for a user",
            description = "Returns all camera-access records for the given user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access list returned (may be empty)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserCameraAccessResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<List<UserCameraAccessResponse>> getAccessList(@PathVariable Long userId) {
        return ResponseEntity.ok(accessControlService.getAccessList(userId));
    }
}
