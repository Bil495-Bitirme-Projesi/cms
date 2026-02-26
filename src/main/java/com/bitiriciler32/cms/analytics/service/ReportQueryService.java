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
                filter.getCameraId(), filter.getFrom(), filter.getTo(), filter.getSeverity());

        Long falsePositiveCount = userAlertAnalyticsRepository.countFalsePositives(
                filter.getCameraId(), filter.getFrom(), filter.getTo());

        List<Object[]> timeData = anomalyEventAnalyticsRepository.countGroupedByTime(
                filter.getCameraId(), filter.getFrom(), filter.getTo(), filter.getSeverity());

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
    public SeverityDistributionResponse getSeverityDistribution(ReportFilterRequest filter) {
        List<Object[]> data = anomalyEventAnalyticsRepository.countGroupedBySeverity(
                filter.getCameraId(), filter.getFrom(), filter.getTo());

        List<SeverityCount> distribution = data.stream()
                .map(row -> new SeverityCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        return new SeverityDistributionResponse(distribution);
    }

    @Transactional(readOnly = true)
    public ModelPerformanceResponse getModelPerformance(ModelPerformanceFilterRequest filter) {
        List<Object[]> data = anomalyEventAnalyticsRepository.countGroupedByModelVersion(
                filter.getModelVersion(), filter.getFrom(), filter.getTo());

        Long totalEvents = 0L;
        for (Object[] row : data) {
            totalEvents += (Long) row[1];
        }

        Long falsePositiveCount = userAlertAnalyticsRepository.countFalsePositives(
                null, filter.getFrom(), filter.getTo());

        Double falsePositiveRate = totalEvents > 0
                ? (double) falsePositiveCount / totalEvents
                : 0.0;

        List<Object[]> timeData = anomalyEventAnalyticsRepository.countGroupedByTime(
                null, filter.getFrom(), filter.getTo(), null);

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

        return new ModelPerformanceResponse(
                filter.getModelVersion(),
                totalEvents,
                falsePositiveCount,
                falsePositiveRate,
                timeSeries
        );
    }
}
