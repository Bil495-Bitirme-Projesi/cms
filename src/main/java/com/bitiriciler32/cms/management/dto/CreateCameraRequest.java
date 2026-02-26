package com.bitiriciler32.cms.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCameraRequest {

    @NotBlank(message = "Camera name is required")
    private String name;

    @NotBlank(message = "RTSP URL is required")
    private String rtspUrl;

    private Boolean detectionEnabled = true;

    @NotNull(message = "Threshold is required")
    private Double threshold;
}
