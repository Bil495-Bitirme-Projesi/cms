package com.bitiriciler32.cms.security.config;

import com.bitiriciler32.cms.security.filter.JwtAuthenticationFilter;
import com.bitiriciler32.cms.security.filter.SubsystemJwtFilter;
import com.bitiriciler32.cms.security.handler.JwtAuthenticationEntryPoint;
import com.bitiriciler32.cms.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 * Stateless JWT-based authentication with role-based access control.
 *
 * Endpoint yetkilendirme mantığı:
 *  - /api/auth/**              → herkese açık (login endpoint'leri)
 *  - /api/events/ingest        → sadece subsystem JWT (scope: inference_sync)
 *  - /ws/**                    → WsJwtHandshakeInterceptor kendi doğrulamasını yapar
 *  - /api/admin/**             → ADMIN rolü zorunlu (@PreAuthorize("hasRole('ADMIN')") ile)
 *  - diğer tüm endpoint'ler   → geçerli kullanıcı JWT'si zorunlu
 *
 * Hata yanıtları:
 *  - Token yok / anonim kullanıcı     → JwtAuthenticationEntryPoint  → 401 + ApiErrorResponse
 *  - Token var ama geçersiz/süresi dolmuş → JwtAuthenticationFilter  → 401 + ApiErrorResponse
 *  - Geçerli token, yetersiz rol (@PreAuthorize ihlali) → GlobalExceptionHandler → 403 + ApiErrorResponse
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SubsystemJwtFilter subsystemJwtFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: login endpoints + health check
                        // /actuator/health/** covers the root endpoint and all group sub-paths
                        // (e.g. /actuator/health/readiness used by Docker healthcheck)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/subsystem-login").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Swagger UI + OpenAPI spec (development convenience)
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Dev-only test endpoints (only active with "dev" profile)
                        .requestMatchers("/api/dev/**").permitAll()
                        // Subsystem endpoint'leri: SubsystemJwtFilter doğrular, burada authenticated yeterli
                        .requestMatchers(HttpMethod.POST, "/api/events/ingest").authenticated()
                        // /api/clips/upload-url has been removed (returns 410 Gone).
                        // Upload URL is now included in the POST /api/events/ingest response.
                        // WebSocket: WsJwtHandshakeInterceptor kendi JWT/scope kontrolünü yapar
                        .requestMatchers("/ws/**").permitAll()
                        // Geri kalan her şey: geçerli kullanıcı JWT'si zorunlu
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(ex -> ex
                        // No token / anonymous user → 401
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // SubsystemJwtFilter runs first (for ingest + upload-url paths only)
                // JwtAuthenticationFilter runs second (for all other authenticated paths)
                // Both sit before UsernamePasswordAuthenticationFilter in the chain.
                // addFilterBefore(A, X) then addFilterBefore(B, A) gives: B → A → X
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(subsystemJwtFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
