package com.bitiriciler32.cms.security.filter;

import com.bitiriciler32.cms.security.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Subsystem JWT authentication filter.
 *
 * Only active for AI node endpoints:
 *   POST /api/events/ingest
 *   POST /api/clips/upload-url
 *
 * Validates that the token is a subsystem JWT (type=subsystem).
 * Token is obtained via POST /api/auth/subsystem-login.
 *
 * This filter runs BEFORE JwtAuthenticationFilter. If the token is a valid
 * subsystem token, it populates the SecurityContext; JwtAuthenticationFilter
 * then sees an already-authenticated context and skips.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubsystemJwtFilter extends OncePerRequestFilter {


    private final JwtTokenService jwtTokenService;

    /**
     * Sadece subsystem endpoint'lerinde çalış; diğerlerini atla.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        return !(
                ("POST".equalsIgnoreCase(method) && "/api/events/ingest".equals(path)) ||
                ("POST".equalsIgnoreCase(method) && "/api/clips/upload-url".equals(path))
        );
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Subsystem endpoint called without Authorization header: {}", request.getServletPath());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Subsystem JWT required. Use /api/auth/subsystem-login to obtain a token.");
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            if (!jwtTokenService.validateSubsystemToken(jwt)) {
                log.warn("Invalid subsystem token for endpoint: {}", request.getServletPath());
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Invalid subsystem token.");
                return;
            }

            // Token geçerli — SecurityContext'e subsystem kimliğini yaz
            String subsystemId = jwtTokenService.extractUsername(jwt);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    subsystemId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SUBSYSTEM"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Subsystem '{}' authenticated successfully → {}", subsystemId, request.getServletPath());

        } catch (Exception e) {
            log.warn("Subsystem token validation error: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Subsystem token validation failed.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

