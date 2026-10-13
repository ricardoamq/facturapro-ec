package com.facturapro.auth;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.UnauthorizedException;
import com.facturapro.config.AppProperties;
import com.facturapro.security.AuthUser;
import com.facturapro.security.CurrentUser;
import com.facturapro.users.User;
import com.facturapro.users.UserRepository;
import com.facturapro.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final AppProperties appProperties;

    @Transactional
    public AuthDtos.SessionResponse login(AuthDtos.LoginRequest request) {
        String username = request.username().trim();
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
            AuthUser authUser = (AuthUser) authentication.getPrincipal();
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = userRepository.findByUsername(authUser.getUsername()).orElseThrow();
            userService.recordSuccessfulLogin(user);
            auditService.recordAs(user.getUsername(), user.getId(), "LOGIN_SUCCESS", "SESSION", user.getId(),
                    "Inicio de sesion correcto");
            return sessionResponse(authUser);
        } catch (DisabledException ex) {
            auditFailed(username, "Usuario desactivado");
            throw new UnauthorizedException("El usuario esta desactivado. Contacte al administrador.");
        } catch (LockedException ex) {
            auditFailed(username, "Usuario bloqueado");
            throw new UnauthorizedException("La cuenta esta bloqueada por intentos fallidos. Espere unos minutos e intente nuevamente.");
        } catch (BadCredentialsException ex) {
            handleBadCredentials(username);
            throw new UnauthorizedException("Usuario o contrasena incorrectos.");
        } catch (AuthenticationException ex) {
            auditFailed(username, ex.getMessage());
            throw new UnauthorizedException("No se pudo iniciar sesion.");
        }
    }

    private void handleBadCredentials(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        user.ifPresent(u -> userService.recordFailedLogin(u,
                appProperties.login().maxFailedAttempts(), appProperties.login().lockMinutes()));
        auditFailed(username, "Credenciales invalidas");
    }

    @Transactional
    public void logout() {
        try {
            AuthUser user = currentUser.require();
            auditService.record("LOGOUT", "SESSION", user.getId(), "Cierre de sesion");
        } catch (Exception ignored) {
            // Sesion ya invalida.
        }
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public AuthDtos.SessionResponse me() {
        return sessionResponse(currentUser.require());
    }

    /**
     * Preparado para envio de correo: el token se genera y guarda hasheado.
     * Sin servidor SMTP configurado el enlace se registra en el log del backend
     * (solo si app.mail.dev-logging-enabled=true).
     */
    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        String email = request.email().trim();
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isActive()) {
                return;
            }
            byte[] raw = new byte[32];
            RANDOM.nextBytes(raw);
            String token = HexFormat.of().formatHex(raw);
            user.setPasswordResetToken(sha256(token));
            user.setPasswordResetExpires(Instant.now().plusSeconds(appProperties.password().resetTtlMinutes() * 60L));
            userRepository.save(user);
            auditService.recordAs(user.getUsername(), user.getId(), "PASSWORD_RESET_REQUESTED", "USER", user.getId(),
                    "Solicitud de recuperacion de contrasena");
            if (appProperties.mail() != null && appProperties.mail().devLoggingEnabled()) {
                log.warn("[DEV] Token de recuperacion para {} ({}): {}", user.getEmail(), user.getUsername(), token);
            }
        });
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        String hashed = sha256(request.token().trim());
        User user = userRepository.findByPasswordResetToken(hashed)
                .filter(u -> u.getPasswordResetExpires() != null && u.getPasswordResetExpires().isAfter(Instant.now()))
                .orElseThrow(() -> new UnauthorizedException("El enlace de recuperacion no es valido o ha expirado."));
        user.setPasswordHash(com.facturapro.config.SecurityConfig.ENCODER.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        user.setMustChangePassword(false);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        auditService.recordAs(user.getUsername(), user.getId(), "PASSWORD_RESET_COMPLETED", "USER", user.getId(),
                "Contrasena restablecida mediante enlace de recuperacion");
    }

    private AuthDtos.SessionResponse sessionResponse(AuthUser authUser) {
        return new AuthDtos.SessionResponse(
                authUser.getId(),
                authUser.getUsername(),
                authUser.getFullName(),
                authUser.getRole().name(),
                authUser.isMustChangePassword(),
                authUser.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet()));
    }

    private void auditFailed(String username, String reason) {
        auditService.recordAs(username, null, "LOGIN_FAILED", "SESSION", null, reason);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
