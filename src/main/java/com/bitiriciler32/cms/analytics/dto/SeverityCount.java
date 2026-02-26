package com.bitiriciler32.cms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeverityCount {
    private String severity;
    private Long count;
}
