package com.bitiriciler32.cms.security.service;

import com.bitiriciler32.cms.security.model.CmsUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT token generation and validation service.
 */
@Slf4j
@Service
public class JwtTokenService {

    private static final String CLAIM_TOKEN_VERSION = "tv";
    private static final String PLACEHOLDER = "CHANGE_ME";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.subsystem-expiration-ms}")
    private long subsystemExpirationMs;

    /**
     * Generate a JWT for a regular user.
     */
    @PostConstruct
    public void validateSecretOnStartup() {
        if (secretKey == null || secretKey.contains(PLACEHOLDER) || secretKey.length() < 32) {
            log.warn("⚠️  JWT secret is using an insecure placeholder value! " +
                     "Set the JWT_SECRET environment variable in production.");
        } else {
            log.info("✅ JWT secret loaded successfully.");
        }
    }

    /**
     * Generate a JWT for a regular user.
     * Embeds the user's current tokenVersion so the server can invalidate
     * older tokens after a login, password change, or role change.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        if (userDetails instanceof CmsUserDetails cmsUser) {
            claims.put(CLAIM_TOKEN_VERSION, cmsUser.getTokenVersion());
        }
        return buildToken(claims, userDetails.getUsername(), expirationMs);
    }

    /**
     * Generate a JWT for a subsystem (e.g., AI Inference Node).
     * The token carries type=subsystem so it can be distinguished from user tokens.
     */
    public String generateSubsystemToken(String subsystemId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "subsystem");
        return buildToken(claims, subsystemId, subsystemExpirationMs);
    }

    /**
     * Validates a user token.
     * In addition to expiry and subject checks, verifies that the tokenVersion
     * embedded in the JWT matches the current value in the database (via UserDetails).
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        if (userDetails instanceof CmsUserDetails cmsUser) {
            Long tokenVersion = extractTokenVersion(token);
            return tokenVersion != null && tokenVersion == cmsUser.getTokenVersion();
        }
        return true;
    }

    /**
     * Validate that a token is a non-expired subsystem token.
     */
    public boolean validateSubsystemToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String type = claims.get("type", String.class);
            return "subsystem".equals(type) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractTokenVersion(String token) {
        return extractClaim(token, claims -> {
            Object tv = claims.get(CLAIM_TOKEN_VERSION);
            if (tv instanceof Number n) return n.longValue();
            return null;
        });
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    // ── Private helpers ──

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
