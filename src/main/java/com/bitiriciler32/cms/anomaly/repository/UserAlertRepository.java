package com.bitiriciler32.cms.anomaly.repository;

import com.bitiriciler32.cms.anomaly.entity.AlertStatus;
import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAlertRepository extends JpaRepository<UserAlertEntity, Long>,
                                              JpaSpecificationExecutor<UserAlertEntity> {

    List<UserAlertEntity> findByUser(UserEntity user);

    List<UserAlertEntity> findByUserAndStatus(UserEntity user, AlertStatus status);

    Optional<UserAlertEntity> findByIdAndUser(Long alertId, UserEntity user);

    List<UserAlertEntity> findByEvent(AnomalyEventEntity event);

    List<UserAlertEntity> findByEventId(Long eventId);
}
