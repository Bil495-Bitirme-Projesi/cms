package com.bitiriciler32.cms.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSnapshot {
    private List<CameraConfigDto> cameras;
}
