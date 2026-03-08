package com.bitiriciler32.cms.security.config;

import com.bitiriciler32.cms.security.filter.JwtAuthenticationFilter;
import com.bitiriciler32.cms.security.filter.SubsystemJwtFilter;
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
 *  - /api/clips/upload-url     → sadece subsystem JWT (scope: inference_sync)
 *  - /ws/**                    → WsJwtHandshakeInterceptor kendi doğrulamasını yapar
 *  - /api/admin/**             → ADMIN rolü zorunlu
 *  - diğer tüm endpoint'ler   → geçerli kullanıcı JWT'si zorunlu
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SubsystemJwtFilter subsystemJwtFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: login endpoints + health check
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Dev-only test endpoints (only active with "dev" profile)
                        .requestMatchers("/api/dev/**").permitAll()
                        // Subsystem endpoint'leri: SubsystemJwtFilter doğrular, burada authenticated yeterli
                        .requestMatchers(HttpMethod.POST, "/api/events/ingest").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/clips/upload-url").authenticated()
                        // WebSocket: WsJwtHandshakeInterceptor kendi JWT/scope kontrolünü yapar
                        .requestMatchers("/ws/**").permitAll()
                        // Sadece ADMIN rolü
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Geri kalan her şey: geçerli kullanıcı JWT'si zorunlu
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
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
