package com.bitiriciler32.cms.anomaly.dto;

import com.bitiriciler32.cms.anomaly.entity.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertQueryFilter {
    private AlertStatus status;
    private Long cameraId;
    private Instant from;
    private Instant to;
    private String type;
}
