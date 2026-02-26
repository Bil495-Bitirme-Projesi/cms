package com.bitiriciler32.cms.media.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlRequest {
    private Long cameraId;
    private Long eventId;
}
