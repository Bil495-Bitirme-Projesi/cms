package com.bitiriciler32.cms.management.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents an IP camera registered in the system.
 */
@Entity
@Table(name = "cameras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CameraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String rtspUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean detectionEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private Double threshold = 0.5;
}
