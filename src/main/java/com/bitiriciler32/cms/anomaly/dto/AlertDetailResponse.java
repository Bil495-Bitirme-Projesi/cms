package com.bitiriciler32.cms.anomaly.dto;

import com.bitiriciler32.cms.anomaly.entity.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertDetailResponse {
    private Long id;
    private Long eventId;
    private Long cameraId;
    private Instant timestamp;
    private Double score;
    private String type;
    private String description;
    private String clipObjectKey;
    private AlertStatus status;
}
