package com.bitiriciler32.cms.anomaly.service;

import com.bitiriciler32.cms.anomaly.entity.AlertStatus;
import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertCommandService {

    private final UserAlertRepository userAlertRepository;

    /**
     * Create one UserAlertEntity per recipient user for the given event.
     */
    @Transactional
    public void createAlerts(AnomalyEventEntity event, List<UserEntity> recipients) {
        for (UserEntity user : recipients) {
            UserAlertEntity alert = UserAlertEntity.builder()
                    .status(AlertStatus.UNSEEN)
                    .user(user)
                    .event(event)
                    .build();
            userAlertRepository.save(alert);
        }
    }

    @Transactional
    public void acknowledge(Long alertId, UserEntity user) {
        UserAlertEntity alert = userAlertRepository.findByIdAndUser(alertId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        userAlertRepository.save(alert);
    }
}
