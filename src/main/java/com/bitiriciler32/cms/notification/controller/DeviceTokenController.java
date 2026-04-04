package com.bitiriciler32.cms.notification.controller;

import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import com.bitiriciler32.cms.notification.dto.RegisterTokenRequest;
import com.bitiriciler32.cms.notification.entity.DeviceFcmTokenEntity;
import com.bitiriciler32.cms.notification.repository.DeviceFcmTokenRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceFcmTokenRepository deviceFcmTokenRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RegisterTokenRequest request) {

        UserEntity user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database"));

        DeviceFcmTokenEntity token = DeviceFcmTokenEntity.builder()
                .user(user)
                .fcmToken(request.getFcmToken())
                .enabled(true)
                .build();

        deviceFcmTokenRepository.save(token);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
