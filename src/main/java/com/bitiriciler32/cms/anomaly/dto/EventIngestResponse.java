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
    /** MinIO object key where AIS should PUT the video clip. */
    private String clipObjectKey;
    /** Presigned PUT URL — valid for {@code clipUploadExpiresInSeconds} seconds. */
    private String clipUploadUrl;
    private Long clipUploadExpiresInSeconds;
}
