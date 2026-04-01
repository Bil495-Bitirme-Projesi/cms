package com.bitiriciler32.cms.anomaly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventIngestRequest {

    @NotBlank(message = "sourceEventId is required")
    private String sourceEventId;

    @NotNull(message = "cameraId is required")
    private Long cameraId;

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    @NotNull(message = "score is required")
    private Double score;

    @NotBlank(message = "severity is required")
    private String severity;

    @NotBlank(message = "type is required")
    private String type;

    private String modelVersion;
}
