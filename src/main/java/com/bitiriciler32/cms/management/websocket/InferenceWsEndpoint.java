package com.bitiriciler32.cms.management.websocket;

import com.bitiriciler32.cms.management.dto.ConfigSyncRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket endpoint for AI Inference Node configuration synchronization.
 * Handles connection lifecycle and incoming messages (e.g., SNAPSHOT requests).
 */
@RequiredArgsConstructor
@Slf4j
public class InferenceWsEndpoint extends TextWebSocketHandler {

    private final ConfigSyncService configSyncService;
    private final InferenceWsSender inferenceWsSender;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        String sessionId = session.getId();
        inferenceWsSender.registerSession(sessionId, session);
        log.info("AI Inference node connected via WebSocket: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,
                                      @NonNull TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        try {
            ConfigSyncRequest request = objectMapper.readValue(payload, ConfigSyncRequest.class);

            if ("SNAPSHOT".equalsIgnoreCase(request.getType())) {
                configSyncService.sendSnapshot(sessionId);
            } else {
                log.warn("Unknown config sync request type: {}", request.getType());
            }
        } catch (Exception e) {
            log.error("Failed to process WebSocket message from session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,
                                       @NonNull CloseStatus status) {
        String sessionId = session.getId();
        inferenceWsSender.removeSession(sessionId);
        log.info("AI Inference node disconnected: {} (status: {})", sessionId, status);
    }
}
