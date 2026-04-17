package com.bitiriciler32.cms.support;

import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.security.model.CmsUserDetails;
import com.bitiriciler32.cms.security.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Test-only helper that generates JWT tokens for use in MockMvc requests.
 *
 * <p>Tokens are generated via the real {@link JwtTokenService} so they are
 * structurally identical to production tokens and pass all filter chain checks.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   mockMvc.perform(get("/api/cameras")
 *       .header("Authorization", "Bearer " + tokenFactory.forUser(adminUser)));
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class TestTokenFactory {

    private final JwtTokenService jwtTokenService;

    /**
     * Generates a user JWT for the given {@link UserEntity}.
     * The token carries the user's current {@code tokenVersion}, matching what
     * the JwtAuthenticationFilter will validate.
     */
    public String forUser(UserEntity user) {
        return jwtTokenService.generateToken(new CmsUserDetails(user));
    }

    /**
     * Generates a subsystem JWT using the configured subsystem ID.
     */
    public String forSubsystem() {
        return jwtTokenService.generateSubsystemToken("test-subsystem");
    }
}

