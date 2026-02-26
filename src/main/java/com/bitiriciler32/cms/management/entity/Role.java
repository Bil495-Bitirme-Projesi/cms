package com.bitiriciler32.cms.management.entity;

/**
 * System roles.
 * Per LLD design decision: simple enum instead of a UserRoleEntity table,
 * since only two roles exist and no runtime role management is needed.
 */
public enum Role {
    ADMIN,
    OPERATOR
}
