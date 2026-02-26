package com.bitiriciler32.cms.analytics.repository;

import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface UserAlertAnalyticsRepository extends JpaRepository<UserAlertEntity, Long> {

    @Query("SELECT COUNT(a) FROM UserAlertEntity a " +
           "WHERE a.falsePositive = true " +
           "AND (:cameraId IS NULL OR a.event.camera.id = :cameraId) " +
           "AND (:from IS NULL OR a.event.timestamp >= :from) " +
           "AND (:to IS NULL OR a.event.timestamp <= :to)")
    Long countFalsePositives(@Param("cameraId") Long cameraId,
                              @Param("from") Instant from,
                              @Param("to") Instant to);
}
