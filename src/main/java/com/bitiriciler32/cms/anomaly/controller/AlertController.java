package com.bitiriciler32.cms.anomaly.controller;

import com.bitiriciler32.cms.anomaly.dto.*;
import com.bitiriciler32.cms.anomaly.service.AlertCommandService;
import com.bitiriciler32.cms.anomaly.service.AlertQueryService;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertQueryService alertQueryService;
    private final AlertCommandService alertCommandService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AlertSummaryResponse>> getAlerts(
            @AuthenticationPrincipal UserDetails userDetails,
            AlertQueryFilter filter) {
        UserEntity user = resolveUser(userDetails);
        return ResponseEntity.ok(alertQueryService.getAlerts(user, filter));
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<AlertDetailResponse> getAlertDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long alertId) {
        UserEntity user = resolveUser(userDetails);
        return ResponseEntity.ok(alertQueryService.getAlertDetail(alertId, user));
    }

    @PatchMapping("/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long alertId) {
        UserEntity user = resolveUser(userDetails);
        alertCommandService.acknowledge(alertId, user);
        return ResponseEntity.ok().build();
    }

    private UserEntity resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}
