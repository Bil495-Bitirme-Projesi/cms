package com.bitiriciler32.cms.analytics.repository;

import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AnomalyEventAnalyticsRepository extends JpaRepository<AnomalyEventEntity, Long> {

    @Query("SELECT COUNT(e) FROM AnomalyEventEntity e " +
           "WHERE (:cameraId IS NULL OR e.camera.id = :cameraId) " +
           "AND (:from IS NULL OR e.timestamp >= :from) " +
           "AND (:to IS NULL OR e.timestamp <= :to)")
    Long countEvents(@Param("cameraId") Long cameraId,
                     @Param("from") Instant from,
                     @Param("to") Instant to);

    @Query("SELECT e.type, COUNT(e) FROM AnomalyEventEntity e " +
           "WHERE (:cameraId IS NULL OR e.camera.id = :cameraId) " +
           "AND (:from IS NULL OR e.timestamp >= :from) " +
           "AND (:to IS NULL OR e.timestamp <= :to) " +
           "GROUP BY e.type")
    List<Object[]> countGroupedByType(@Param("cameraId") Long cameraId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    @Query("SELECT CAST(e.timestamp AS date), COUNT(e) FROM AnomalyEventEntity e " +
           "WHERE (:cameraId IS NULL OR e.camera.id = :cameraId) " +
           "AND (:from IS NULL OR e.timestamp >= :from) " +
           "AND (:to IS NULL OR e.timestamp <= :to) " +
           "GROUP BY CAST(e.timestamp AS date) ORDER BY CAST(e.timestamp AS date)")
    List<Object[]> countGroupedByTime(@Param("cameraId") Long cameraId,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);
}
