package com.bitiriciler32.cms.management.dto;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraResponse {
    private Long id;
    private String name;
    private String rtspUrl;
    private Boolean detectionEnabled;
    private Double threshold;

    public static CameraResponse fromEntity(CameraEntity entity) {
        return new CameraResponse(
                entity.getId(),
                entity.getName(),
                entity.getRtspUrl(),
                entity.getDetectionEnabled(),
                entity.getThreshold()
        );
    }
}
