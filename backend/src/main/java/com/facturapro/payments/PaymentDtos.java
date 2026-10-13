package com.facturapro.payments;

import com.facturapro.sales.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDtos() {

    public record RegisterRequest(
            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero") BigDecimal amount,
            @NotNull(message = "El metodo de pago es obligatorio") PaymentMethod method,
            @Size(max = 60) String reference,
            @Size(max = 255) String notes) {
    }

    public record VoidRequest(@NotNull @Size(max = 255) String reason) {
    }

    public record Response(
            Long id,
            Long saleId,
            String saleNumber,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String reference,
            String notes,
            Instant paymentDate,
            boolean voided,
            String voidReason,
            Instant voidedAt,
            String createdByName) {

        public static Response from(Payment p) {
            return new Response(
                    p.getId(),
                    p.getSale().getId(),
                    p.getSale().getNumber(),
                    p.getAmount(),
                    p.getPaymentMethod(),
                    p.getReference(),
                    p.getNotes(),
                    p.getPaymentDate(),
                    p.isVoided(),
                    p.getVoidReason(),
                    p.getVoidedAt(),
                    p.getCreatedByName());
        }
    }
}
