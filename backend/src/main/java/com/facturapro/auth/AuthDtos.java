package com.facturapro.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthDtos() {

    public record LoginRequest(
            @NotBlank(message = "El usuario es obligatorio") String username,
            @NotBlank(message = "La contrasena es obligatoria") String password) {
    }

    public record SessionResponse(
            Long id,
            String username,
            String fullName,
            String role,
            boolean mustChangePassword,
            java.util.Set<String> permissions) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "El correo electronico es obligatorio") @Size(max = 120) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "El token es obligatorio") String token,
            @NotBlank(message = "La nueva contrasena es obligatoria") @Size(min = 8, max = 72) String newPassword) {
    }
}
