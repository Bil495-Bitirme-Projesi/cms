package com.bitiriciler32.cms.management.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Maps which operators have access to which cameras.
 * Determines alert routing: operators only receive alerts for cameras they are assigned to.
 */
@Entity
@Table(name = "user_camera_access",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "camera_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCameraAccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camera_id", nullable = false)
    private CameraEntity camera;
}
