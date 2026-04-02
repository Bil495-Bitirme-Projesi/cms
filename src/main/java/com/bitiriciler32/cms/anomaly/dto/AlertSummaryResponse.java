package com.bitiriciler32.cms.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryResponse {
    private Long alertId;
    private Long eventId;
    private Long cameraId;
    private Instant timestamp;
    private String type;
    private Double score;
    private String description;
    private String status;
}
