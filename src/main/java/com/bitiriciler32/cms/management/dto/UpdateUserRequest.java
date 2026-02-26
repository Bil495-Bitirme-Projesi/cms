package com.bitiriciler32.cms.management.dto;

import com.bitiriciler32.cms.management.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String name;
    private Boolean enabled;
    private Role role;
}
