package com.bitiriciler32.cms.management.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Actuator health indicator for the AIS WebSocket connection.
 *
 * Visible at GET /actuator/health under the "aisWebSocket" component key.
 *
 * Status:
 *   UP      — AIS is connected and sending messages recently
 *   UNKNOWN — AIS is connected but has been silent longer than SILENCE_THRESHOLD
 *             (connection open but AIS may be frozen or heartbeats stopped)
 *   DOWN    — No AIS node is connected
 */
@Component("aisWebSocket")
@RequiredArgsConstructor
public class AisWebSocketHealthIndicator implements HealthIndicator {

    /**
     * If no message has been received for this long despite an open connection,
     * report UNKNOWN — something may be wrong on the AIS side.
     * Should be set to a few multiples of the AIS heartbeat interval.
     */
    private static final Duration SILENCE_THRESHOLD = Duration.ofMinutes(3);

    private final InferenceWsSender inferenceWsSender;

    @Override
    public Health health() {
        int connected = inferenceWsSender.getConnectedSessionCount();
        Instant lastMessage = inferenceWsSender.getLastMessageReceivedAt();

        if (connected == 0) {
            return Health.down()
                    .withDetail("connectedSessions", 0)
                    .withDetail("reason", "No AIS node is currently connected via WebSocket")
                    .build();
        }

        // Connected — check if messages are still flowing
        if (lastMessage == null) {
            // Connected but never sent a message yet (just connected, SNAPSHOT not sent yet)
            return Health.unknown()
                    .withDetail("connectedSessions", connected)
                    .withDetail("lastMessageReceivedAt", "never")
                    .withDetail("reason", "AIS connected but no message received yet")
                    .build();
        }

        Duration silence = Duration.between(lastMessage, Instant.now());
        if (silence.compareTo(SILENCE_THRESHOLD) > 0) {
            return Health.unknown()
                    .withDetail("connectedSessions", connected)
                    .withDetail("lastMessageReceivedAt", lastMessage.toString())
                    .withDetail("silenceSeconds", silence.toSeconds())
                    .withDetail("reason", "AIS is connected but has been silent for " + silence.toSeconds() + "s")
                    .build();
        }

        return Health.up()
                .withDetail("connectedSessions", connected)
                .withDetail("lastMessageReceivedAt", lastMessage.toString())
                .withDetail("silenceSeconds", silence.toSeconds())
                .build();
    }
}

