package com.bitiriciler32.cms.management.websocket;

import com.bitiriciler32.cms.management.dto.CameraStatusReport;
import com.bitiriciler32.cms.management.service.CameraHealthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket endpoint for AI Inference Node communication.
 * Handles:
 * - SNAPSHOT requests (AI node asks for full camera config)
 * - CAMERA_STATUS reports (AI node reports camera connectivity)
 */
@RequiredArgsConstructor
@Slf4j
public class InferenceWsEndpoint extends TextWebSocketHandler {

    private final ConfigSyncService configSyncService;
    private final InferenceWsSender inferenceWsSender;
    private final CameraHealthService cameraHealthService;
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
        inferenceWsSender.recordMessageReceived();

        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText("");

            switch (type.toUpperCase()) {
                case "SNAPSHOT" -> configSyncService.sendSnapshot(sessionId);
                case "CAMERA_STATUS" -> {
                    CameraStatusReport report = objectMapper.treeToValue(root, CameraStatusReport.class);
                    cameraHealthService.applyStatusReport(report);
                }
                default -> log.warn("Unknown message type '{}' from session {}", type, sessionId);
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
