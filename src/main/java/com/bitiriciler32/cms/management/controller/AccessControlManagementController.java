package com.bitiriciler32.cms.management.controller;

import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/access")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccessControlManagementController {

    private final AccessControlService accessControlService;

    @PostMapping("/grant")
    public ResponseEntity<Void> grantAccess(@RequestParam Long userId,
                                             @RequestParam Long cameraId) {
        accessControlService.grantAccess(userId, cameraId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<Void> revokeAccess(@RequestParam Long userId,
                                              @RequestParam Long cameraId) {
        accessControlService.revokeAccess(userId, cameraId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserCameraAccessEntity>> getAccessList(@PathVariable Long userId) {
        return ResponseEntity.ok(accessControlService.getAccessList(userId));
    }
}
