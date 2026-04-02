package com.bitiriciler32.cms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterRequest {
    private Long cameraId;
    private Instant from;
    private Instant to;
}
