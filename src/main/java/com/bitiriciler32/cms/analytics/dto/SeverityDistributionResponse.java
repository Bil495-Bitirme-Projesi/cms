package com.bitiriciler32.cms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeverityDistributionResponse {
    private List<SeverityCount> distribution;
}
