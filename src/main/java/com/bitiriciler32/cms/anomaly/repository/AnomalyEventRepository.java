package com.bitiriciler32.cms.anomaly.repository;

import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnomalyEventRepository extends JpaRepository<AnomalyEventEntity, Long> {

    Optional<AnomalyEventEntity> findBySourceEventId(String sourceEventId);

    boolean existsBySourceEventId(String sourceEventId);
}
