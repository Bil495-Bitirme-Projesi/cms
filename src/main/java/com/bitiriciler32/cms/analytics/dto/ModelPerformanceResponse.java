package com.bitiriciler32.cms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelPerformanceResponse {
    private String modelVersion;
    private Long totalEvents;
    private Long falsePositiveCount;
    private Double falsePositiveRate;
    private List<TimeSeriesPoint> timeSeries;
}
