package com.bitiriciler32.cms.management.controller;

import com.bitiriciler32.cms.management.dto.CameraResponse;
import com.bitiriciler32.cms.management.dto.CreateCameraRequest;
import com.bitiriciler32.cms.management.dto.UpdateCameraRequest;
import com.bitiriciler32.cms.management.service.CameraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cameras")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CameraManagementController {

    private final CameraService cameraService;

    @PostMapping
    public ResponseEntity<CameraResponse> create(@Valid @RequestBody CreateCameraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cameraService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CameraResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateCameraRequest request) {
        return ResponseEntity.ok(cameraService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cameraService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CameraResponse>> getAll() {
        return ResponseEntity.ok(cameraService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CameraResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cameraService.findById(id));
    }
}
