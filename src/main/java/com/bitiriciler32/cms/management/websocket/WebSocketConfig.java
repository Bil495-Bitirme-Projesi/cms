package com.bitiriciler32.cms.management.websocket;

import com.bitiriciler32.cms.security.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ConfigSyncService configSyncService;
    private final InferenceWsSender inferenceWsSender;
    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(
                        new InferenceWsEndpoint(configSyncService, inferenceWsSender, objectMapper),
                        "/ws/inference-sync")
                .addInterceptors(new WsJwtHandshakeInterceptor(jwtTokenService))
                .setAllowedOrigins("*");
    }
}
