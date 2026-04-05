package com.bitiriciler32.cms.management.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a system user (administrator or security operator).
 * Email is used as the login identifier (per design decision).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * Incremented on every login, password change, or role change.
     * Embedded in the JWT and checked on every request – allows server-side token invalidation.
     */
    @Builder.Default
    @Column(nullable = false)
    private Long tokenVersion = 1L;
}
