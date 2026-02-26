package com.bitiriciler32.cms.analytics.repository;

import com.bitiriciler32.cms.analytics.entity.DailyModelSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyModelSummaryRepository extends JpaRepository<DailyModelSummaryEntity, Long> {

    List<DailyModelSummaryEntity> findByDate(LocalDate date);

    List<DailyModelSummaryEntity> findByDateBetween(LocalDate from, LocalDate to);
}
