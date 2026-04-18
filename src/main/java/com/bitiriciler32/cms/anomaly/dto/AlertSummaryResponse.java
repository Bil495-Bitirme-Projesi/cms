package com.bitiriciler32.cms.anomaly.dto;

import com.bitiriciler32.cms.anomaly.entity.AlertStatus;
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
    private String cameraName;
    private Instant timestamp;
    private String type;
    private Double score;
    private String description;
    private AlertStatus status;
}
