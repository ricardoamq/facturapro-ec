package com.facturapro.security;

import com.facturapro.users.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Role role;
    private final boolean active;
    private final boolean mustChangePassword;
    private final boolean locked;

    public AuthUser(Long id, String username, String password, String fullName, Role role,
                    boolean active, boolean mustChangePassword, boolean locked) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
        this.locked = locked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = role.getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.name()))
                .toList();
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
