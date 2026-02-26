package com.bitiriciler32.cms.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraConfigDto {
    private Long cameraId;
    private String name;
    private String rtspUrl;
    private Boolean detectionEnabled;
    private Double threshold;
}
