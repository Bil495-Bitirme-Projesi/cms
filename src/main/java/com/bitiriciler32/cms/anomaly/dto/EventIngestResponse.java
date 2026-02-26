package com.bitiriciler32.cms.anomaly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventIngestResponse {
    private Long eventId;
    private String status;
}
