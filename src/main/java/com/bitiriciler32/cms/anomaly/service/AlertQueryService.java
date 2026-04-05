package com.bitiriciler32.cms.anomaly.service;

import com.bitiriciler32.cms.anomaly.dto.AlertDetailResponse;
import com.bitiriciler32.cms.anomaly.dto.AlertQueryFilter;
import com.bitiriciler32.cms.anomaly.dto.AlertSummaryResponse;
import com.bitiriciler32.cms.anomaly.entity.AnomalyEventEntity;
import com.bitiriciler32.cms.anomaly.entity.UserAlertEntity;
import com.bitiriciler32.cms.anomaly.repository.UserAlertRepository;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.UserEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertQueryService {

    private final UserAlertRepository userAlertRepository;

    @Transactional(readOnly = true)
    public List<AlertSummaryResponse> getAlerts(UserEntity user, AlertQueryFilter filter) {
        Specification<UserAlertEntity> spec = buildSpecification(user, filter);
        return userAlertRepository.findAll(spec).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertDetailResponse getAlertDetail(Long alertId, UserEntity user) {
        UserAlertEntity alert = userAlertRepository.findByIdAndUser(alertId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));
        return toDetail(alert);
    }

    private Specification<UserAlertEntity> buildSpecification(UserEntity user, AlertQueryFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user"), user));

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                // Join with AnomalyEventEntity for event-level filters
                Join<UserAlertEntity, AnomalyEventEntity> eventJoin = root.join("event");

                if (filter.getCameraId() != null) {
                    predicates.add(cb.equal(eventJoin.get("camera").get("id"),
                            filter.getCameraId()));
                }
                if (filter.getFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(eventJoin.get("timestamp"),
                            filter.getFrom()));
                }
                if (filter.getTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(eventJoin.get("timestamp"),
                            filter.getTo()));
                }
                if (filter.getType() != null) {
                    predicates.add(cb.equal(eventJoin.get("type"),
                            filter.getType()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AlertSummaryResponse toSummary(UserAlertEntity alert) {
        AnomalyEventEntity event = alert.getEvent();
        return new AlertSummaryResponse(
                alert.getId(),
                event.getId(),
                event.getCamera().getId(),
                event.getTimestamp(),
                event.getType(),
                event.getScore(),
                event.getDescription(),
                alert.getStatus()
        );
    }

    private AlertDetailResponse toDetail(UserAlertEntity alert) {
        AnomalyEventEntity event = alert.getEvent();
        return new AlertDetailResponse(
                alert.getId(),
                event.getId(),
                event.getCamera().getId(),
                event.getTimestamp(),
                event.getScore(),
                event.getType(),
                event.getDescription(),
                event.getClipObjectKey(),
                alert.getStatus()
        );
    }
}
