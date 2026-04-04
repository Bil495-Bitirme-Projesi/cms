package com.bitiriciler32.cms.management.controller;

import com.bitiriciler32.cms.common.ApiErrorResponse;
import com.bitiriciler32.cms.management.dto.CameraResponse;
import com.bitiriciler32.cms.management.dto.CreateCameraRequest;
import com.bitiriciler32.cms.management.dto.UpdateCameraRequest;
import com.bitiriciler32.cms.management.service.CameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cameras", description = "Camera management – ADMIN only")
@SecurityRequirement(name = "userAuth")
@RestController
@RequestMapping("/api/admin/cameras")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CameraManagementController {

    private final CameraService cameraService;

    @Operation(summary = "Create camera",
            description = "Registers a new camera. After persisting, the configuration is pushed to the AI Inference "
                    + "node via WebSocket. If the AI node is not connected at that moment the push is silently skipped "
                    + "— it will sync on its next reconnection.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Camera created",
                    content = @Content(schema = @Schema(implementation = CameraResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed request body",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CameraResponse> create(@Valid @RequestBody CreateCameraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cameraService.create(request));
    }

    @Operation(summary = "Update camera",
            description = "Partially updates camera configuration. All fields are optional – only provided fields are applied. "
                    + "On success the updated config is pushed to the AI Inference node via WebSocket.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camera updated",
                    content = @Content(schema = @Schema(implementation = CameraResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request body",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Camera not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CameraResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateCameraRequest request) {
        return ResponseEntity.ok(cameraService.update(id, request));
    }

    @Operation(summary = "Delete camera",
            description = "Permanently deletes a camera. The AI Inference node is notified of the deletion via WebSocket.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Camera deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Camera not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cameraService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List all cameras", description = "Returns all registered cameras. Returns an empty list if none exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camera list returned (may be empty)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CameraResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<CameraResponse>> getAll() {
        return ResponseEntity.ok(cameraService.findAll());
    }

    @Operation(summary = "Get camera by ID", description = "Returns a single camera by its database ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Camera found",
                    content = @Content(schema = @Schema(implementation = CameraResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Camera not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CameraResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cameraService.findById(id));
    }
}
