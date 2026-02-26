package com.bitiriciler32.cms.analytics.service;

import com.bitiriciler32.cms.analytics.entity.DailyModelSummaryEntity;
import com.bitiriciler32.cms.analytics.repository.AnomalyEventAnalyticsRepository;
import com.bitiriciler32.cms.analytics.repository.DailyModelSummaryRepository;
import com.bitiriciler32.cms.analytics.repository.UserAlertAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Computes daily model performance summaries for the analytics dashboard.
 * Runs automatically at midnight every day.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailySummaryScheduler {

    private final AnomalyEventAnalyticsRepository anomalyEventAnalyticsRepository;
    private final UserAlertAnalyticsRepository userAlertAnalyticsRepository;
    private final DailyModelSummaryRepository dailyModelSummaryRepository;

    @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
    public void computeYesterdaySummary() {
        computeDailySummary(LocalDate.now().minusDays(1));
    }

    @Transactional
    public void computeDailySummary(LocalDate date) {
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> modelCounts = anomalyEventAnalyticsRepository
                .countGroupedByModelVersion(null, from, to);

        for (Object[] row : modelCounts) {
            String modelVersion = (String) row[0];
            Long totalEvents = (Long) row[1];

            Long falsePositiveCount = userAlertAnalyticsRepository
                    .countFalsePositives(null, from, to);

            Double falsePositiveRate = totalEvents > 0
                    ? (double) falsePositiveCount / totalEvents
                    : 0.0;

            DailyModelSummaryEntity summary = DailyModelSummaryEntity.builder()
                    .date(date)
                    .modelVersion(modelVersion)
                    .totalEvents(totalEvents)
                    .falsePositiveCount(falsePositiveCount)
                    .falsePositiveRate(falsePositiveRate)
                    .build();

            dailyModelSummaryRepository.save(summary);

            log.info("Daily summary computed for date={}, model={}, events={}, fp={}",
                    date, modelVersion, totalEvents, falsePositiveCount);
        }
    }
}
