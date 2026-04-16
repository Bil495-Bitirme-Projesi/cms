package com.bitiriciler32.cms.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraDelta {
    private String type = "CAMERA_DELTA";
    /** UPSERT or DELETE */
    private String changeType;
    private CameraConfigDto camera;
    private Long cameraId;
}
