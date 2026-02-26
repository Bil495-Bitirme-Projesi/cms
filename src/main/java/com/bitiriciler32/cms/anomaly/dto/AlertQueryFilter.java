package com.bitiriciler32.cms.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertQueryFilter {
    private String status;
    private Long cameraId;
    private Instant from;
    private Instant to;
    private String severity;
    private String type;
}
