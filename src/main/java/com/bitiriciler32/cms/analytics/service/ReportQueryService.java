package com.bitiriciler32.cms.analytics.service;

import com.bitiriciler32.cms.analytics.dto.*;
import com.bitiriciler32.cms.analytics.repository.AnomalyEventAnalyticsRepository;
import com.bitiriciler32.cms.analytics.repository.UserAlertAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only analytical query layer.
 * Does not modify anomaly events or alert state.
 */
@Service
@RequiredArgsConstructor
public class ReportQueryService {

    private final AnomalyEventAnalyticsRepository anomalyEventAnalyticsRepository;
    private final UserAlertAnalyticsRepository userAlertAnalyticsRepository;

    @Transactional(readOnly = true)
    public EventStatisticsResponse getEventStatistics(ReportFilterRequest filter) {
        Long totalEvents = anomalyEventAnalyticsRepository.countEvents(
                filter.getCameraId(), filter.getFrom(), filter.getTo());

        Long falsePositiveCount = userAlertAnalyticsRepository.countFalsePositives(
                filter.getCameraId(), filter.getFrom(), filter.getTo());

        List<Object[]> timeData = anomalyEventAnalyticsRepository.countGroupedByTime(
                filter.getCameraId(), filter.getFrom(), filter.getTo());

        List<TimeSeriesPoint> timeSeries = timeData.stream()
                .map(row -> {
                    Instant ts;
                    if (row[0] instanceof Date) {
                        ts = ((Date) row[0]).toLocalDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
                    } else {
                        ts = (Instant) row[0];
                    }
                    return new TimeSeriesPoint(ts, (Long) row[1]);
                })
                .collect(Collectors.toList());

        return new EventStatisticsResponse(totalEvents, falsePositiveCount, timeSeries);
    }

    @Transactional(readOnly = true)
    public SeverityDistributionResponse getTypeDistribution(ReportFilterRequest filter) {
        List<Object[]> data = anomalyEventAnalyticsRepository.countGroupedByType(
                filter.getCameraId(), filter.getFrom(), filter.getTo());

        List<SeverityCount> distribution = data.stream()
                .map(row -> new SeverityCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        return new SeverityDistributionResponse(distribution);
    }
}
