package com.bitiriciler32.cms.security.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores credentials for subsystem-to-subsystem authentication (e.g., AI Inference Node).
 */
@Entity
@Table(name = "subsystem_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubsystemCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String subsystemId;

    @Column(nullable = false)
    private String subsystemSecret;
}
