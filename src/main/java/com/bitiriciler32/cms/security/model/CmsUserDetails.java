package com.bitiriciler32.cms.security.model;

import com.bitiriciler32.cms.management.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails implementation that also carries the tokenVersion
 * from the database. The version is embedded in JWTs and validated on every
 * authenticated request, enabling server-side token invalidation on login,
 * password change, or role change.
 */
public class CmsUserDetails implements UserDetails {

    private final UserEntity user;

    public CmsUserDetails(UserEntity user) {
        this.user = user;
    }

    public long getTokenVersion() {
        return user.getTokenVersion();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }

    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}

