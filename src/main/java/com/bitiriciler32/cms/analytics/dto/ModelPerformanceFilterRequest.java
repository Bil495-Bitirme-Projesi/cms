package com.bitiriciler32.cms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelPerformanceFilterRequest {
    private String modelVersion;
    private Instant from;
    private Instant to;
}
