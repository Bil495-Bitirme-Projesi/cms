package com.bitiriciler32.cms.security.service;

import com.bitiriciler32.cms.security.dto.AuthResponse;
import com.bitiriciler32.cms.security.dto.LoginRequest;
import com.bitiriciler32.cms.security.dto.SubsystemLoginRequest;
import com.bitiriciler32.cms.security.entity.SubsystemCredentialEntity;
import com.bitiriciler32.cms.security.repository.SubsystemCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Handles user and subsystem authentication, issuing JWT tokens on success.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SubsystemCredentialRepository subsystemCredentialRepository;

    /**
     * Authenticate a user with email + password and return a JWT.
     */
    public AuthResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtTokenService.generateToken(userDetails);

        return new AuthResponse(token, jwtTokenService.getExpirationMs());
    }

    /**
     * Authenticate a subsystem (e.g., AI Inference Node) with subsystemId + secret.
     * Returns a JWT containing the subsystem's scope.
     */
    public AuthResponse authenticateSubsystem(SubsystemLoginRequest request) {
        SubsystemCredentialEntity credential = subsystemCredentialRepository
                .findBySubsystemId(request.getSubsystemId())
                .orElseThrow(() -> new BadCredentialsException("Invalid subsystem credentials"));

        if (!credential.getSubsystemSecret().equals(request.getSubsystemSecret())) {
            throw new BadCredentialsException("Invalid subsystem credentials");
        }

        String token = jwtTokenService.generateSubsystemToken(
                credential.getSubsystemId(), credential.getScope());

        return new AuthResponse(token, jwtTokenService.getExpirationMs());
    }
}
