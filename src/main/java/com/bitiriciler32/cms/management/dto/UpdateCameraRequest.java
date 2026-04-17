package com.bitiriciler32.cms.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCameraRequest {
    private String name;
    private String rtspUrl;
    private Boolean detectionEnabled;
}
