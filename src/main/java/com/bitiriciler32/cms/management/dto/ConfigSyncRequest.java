package com.bitiriciler32.cms.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSyncRequest {
    /** e.g., "SNAPSHOT" */
    private String type;
}
