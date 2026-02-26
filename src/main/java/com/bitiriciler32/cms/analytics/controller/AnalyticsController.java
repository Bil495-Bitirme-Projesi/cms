package com.bitiriciler32.cms.analytics.controller;

import com.bitiriciler32.cms.analytics.dto.*;
import com.bitiriciler32.cms.analytics.service.ReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ReportQueryService reportQueryService;

    @GetMapping("/events")
    public ResponseEntity<EventStatisticsResponse> getEventStatistics(ReportFilterRequest filter) {
        return ResponseEntity.ok(reportQueryService.getEventStatistics(filter));
    }

    @GetMapping("/severity")
    public ResponseEntity<SeverityDistributionResponse> getSeverityDistribution(
            ReportFilterRequest filter) {
        return ResponseEntity.ok(reportQueryService.getSeverityDistribution(filter));
    }

    @GetMapping("/model-performance")
    public ResponseEntity<ModelPerformanceResponse> getModelPerformance(
            ModelPerformanceFilterRequest filter) {
        return ResponseEntity.ok(reportQueryService.getModelPerformance(filter));
    }
}
