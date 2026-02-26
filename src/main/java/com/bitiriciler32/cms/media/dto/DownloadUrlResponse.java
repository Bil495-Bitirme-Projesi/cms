package com.bitiriciler32.cms.media.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadUrlResponse {
    private String downloadUrl;
    private Long expiresInSeconds;
}
