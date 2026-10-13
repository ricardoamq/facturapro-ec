package com.facturapro.security;

import com.facturapro.users.User;
import com.facturapro.users.UserRepository;
import com.facturapro.users.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true
    )
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.isMustChangePassword(),
                user.isLocked());
    }
}
