package com.bitiriciler32.cms.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a user–camera access record.
 * Contains only the information needed by the caller; no sensitive entity fields are exposed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCameraAccessResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long cameraId;
    private String cameraName;
}

