package com.facturapro.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDtos() {

    public record CreateRequest(
            @NotBlank(message = "El usuario es obligatorio") @Size(min = 3, max = 50) String username,
            @NotBlank(message = "El nombre completo es obligatorio") @Size(max = 120) String fullName,
            @Email(message = "El correo electronico no es valido") @Size(max = 120) String email,
            @Size(max = 30) String phone,
            @NotNull(message = "El rol es obligatorio") Role role,
            @NotBlank(message = "La contrasena es obligatoria") @Size(min = 8, max = 72) String password) {
    }

    public record UpdateRequest(
            @NotBlank(message = "El nombre completo es obligatorio") @Size(max = 120) String fullName,
            @Email(message = "El correo electronico no es valido") @Size(max = 120) String email,
            @Size(max = 30) String phone,
            @NotNull(message = "El rol es obligatorio") Role role,
            Boolean active) {
    }

    public record Response(
            Long id,
            String username,
            String fullName,
            String email,
            String phone,
            Role role,
            boolean active,
            boolean mustChangePassword,
            java.time.Instant lastLoginAt,
            java.time.Instant createdAt) {

        public static Response from(User user) {
            return new Response(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                    user.getPhone(), user.getRole(), user.isActive(), user.isMustChangePassword(),
                    user.getLastLoginAt(), user.getCreatedAt());
        }
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "La contrasena actual es obligatoria") String currentPassword,
            @NotBlank(message = "La nueva contrasena es obligatoria") @Size(min = 8, max = 72) String newPassword) {
    }

    public record AdminResetPasswordRequest(
            @NotBlank @Size(min = 8, max = 72) String newPassword,
            Boolean mustChangePassword) {
    }
}
