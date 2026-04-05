package com.bitiriciler32.cms.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight camera identifier returned by GET /api/cameras/my.
 * Used by clients to populate camera filter dropdowns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraInfo {
    private Long id;
    private String name;
}

