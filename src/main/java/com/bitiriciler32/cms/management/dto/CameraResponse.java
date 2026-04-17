package com.bitiriciler32.cms.management.dto;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraResponse {
    private Long id;
    private String name;
    private String rtspUrl;
    private Boolean detectionEnabled;
    private StreamStatus streamStatus;
    private Instant lastHeartbeatAt;

    public static CameraResponse fromEntity(CameraEntity entity) {
        return new CameraResponse(
                entity.getId(),
                entity.getName(),
                entity.getRtspUrl(),
                entity.getDetectionEnabled(),
                entity.getStreamStatus(),
                entity.getLastHeartbeatAt()
        );
    }
}
