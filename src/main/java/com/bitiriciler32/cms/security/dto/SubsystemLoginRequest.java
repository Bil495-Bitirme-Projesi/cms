package com.bitiriciler32.cms.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubsystemLoginRequest {

    @NotBlank(message = "Subsystem ID is required")
    private String subsystemId;

    @NotBlank(message = "Subsystem secret is required")
    private String subsystemSecret;
}
