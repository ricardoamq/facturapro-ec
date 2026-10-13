package com.facturapro.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthDtos.SessionResponse login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                          HttpServletRequest httpRequest) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthDtos.SessionResponse me() {
        return authService.me();
    }

    /** Emite la cookie XSRF-TOKEN (leible por JS) que el frontend devuelve en X-XSRF-TOKEN. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Si el correo existe, se enviara un enlace de recuperacion."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Contrasena actualizada. Ya puede iniciar sesion."));
    }
}
