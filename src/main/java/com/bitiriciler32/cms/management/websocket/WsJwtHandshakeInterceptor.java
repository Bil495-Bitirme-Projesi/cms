package com.bitiriciler32.cms.management.websocket;

import com.bitiriciler32.cms.security.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * Validates JWT during WebSocket handshake.
 * Only tokens with the required scope (e.g., "inference_sync") are accepted.
 */
@RequiredArgsConstructor
@Slf4j
public class WsJwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenService jwtTokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                    ServerHttpResponse response,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {
        try {
            URI uri = request.getURI();
            String query = uri.getQuery();

            if (query == null || !query.contains("token=")) {
                log.warn("WebSocket handshake rejected: no token provided");
                return false;
            }

            String token = extractTokenFromQuery(query);

            if (jwtTokenService.validateSubsystemToken(token, "inference_sync")) {
                log.info("WebSocket handshake accepted for subsystem");
                return true;
            }

            log.warn("WebSocket handshake rejected: invalid or unauthorized token");
            return false;
        } catch (Exception e) {
            log.error("WebSocket handshake error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                                ServerHttpResponse response,
                                WebSocketHandler wsHandler,
                                Exception exception) {
        // No-op
    }

    private String extractTokenFromQuery(String query) {
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring(6);
            }
        }
        return "";
    }
}
