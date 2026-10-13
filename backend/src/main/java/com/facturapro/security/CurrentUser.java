package com.facturapro.security;

import com.facturapro.common.error.UnauthorizedException;
import com.facturapro.users.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AuthUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw new UnauthorizedException("Debe iniciar sesion para acceder a este recurso.");
        }
        return user;
    }

    public Long id() {
        return require().getId();
    }

    public String username() {
        return require().getUsername();
    }

    public Role role() {
        return require().getRole();
    }
}
