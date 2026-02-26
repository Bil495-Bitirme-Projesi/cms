package com.bitiriciler32.cms.security.controller;

import com.bitiriciler32.cms.security.dto.AuthResponse;
import com.bitiriciler32.cms.security.dto.LoginRequest;
import com.bitiriciler32.cms.security.dto.SubsystemLoginRequest;
import com.bitiriciler32.cms.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/subsystem-login")
    public ResponseEntity<AuthResponse> subsystemLogin(
            @Valid @RequestBody SubsystemLoginRequest request) {
        return ResponseEntity.ok(authService.authenticateSubsystem(request));
    }
}
