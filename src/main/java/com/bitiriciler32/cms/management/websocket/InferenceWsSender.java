package com.bitiriciler32.cms.management.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active WebSocket sessions and sends messages to connected AI nodes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InferenceWsSender {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
        log.info("WebSocket session registered: {}", sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("WebSocket session removed: {}", sessionId);
    }

    /**
     * Send a message to a specific session.
     */
    public void send(String sessionId, Object msg) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(msg);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to session {}: {}", sessionId, e.getMessage());
            }
        }
    }

    /**
     * Broadcast a message to all connected sessions.
     */
    public void broadcast(Object msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            TextMessage textMessage = new TextMessage(json);
            for (var entry : sessions.entrySet()) {
                WebSocketSession session = entry.getValue();
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("Failed to broadcast to session {}: {}", entry.getKey(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize broadcast message: {}", e.getMessage());
        }
    }
}
