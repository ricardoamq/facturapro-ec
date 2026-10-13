package com.facturapro.suppliers;

import com.facturapro.customers.IdType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SupplierDtos() {

    public record Request(
            @NotNull(message = "El tipo de identificacion es obligatorio") IdType idType,
            @NotBlank(message = "La identificacion es obligatoria")
            @Pattern(regexp = "[0-9A-Za-z\\-]{5,20}", message = "Identificacion invalida") String identification,
            @NotBlank(message = "El nombre o razon social es obligatorio") @Size(max = 160) String businessName,
            @Size(max = 255) String address,
            @Size(max = 30) String phone,
            @Email(message = "El correo electronico no es valido") @Size(max = 120) String email,
            @Size(max = 500) String notes,
            Boolean active) {
    }

    public record Response(
            Long id,
            IdType idType,
            String identification,
            String businessName,
            String address,
            String phone,
            String email,
            String notes,
            boolean active,
            Instant createdAt) {

        public static Response from(Supplier s) {
            return new Response(s.getId(), s.getIdType(), s.getIdentification(), s.getBusinessName(),
                    s.getAddress(), s.getPhone(), s.getEmail(), s.getNotes(), s.isActive(), s.getCreatedAt());
        }
    }
}
