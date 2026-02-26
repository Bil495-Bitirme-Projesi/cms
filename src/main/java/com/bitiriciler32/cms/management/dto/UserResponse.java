package com.bitiriciler32.cms.management.dto;

import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Boolean enabled;

    public static UserResponse fromEntity(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getRole(),
                entity.getEnabled()
        );
    }
}
