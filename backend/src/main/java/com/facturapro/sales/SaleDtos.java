package com.facturapro.sales;

import com.facturapro.payments.PaymentDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SaleDtos() {

    public record ItemRequest(
            @NotNull(message = "Indique el producto") Long productId,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero") BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercent) {
    }

    public record CreateRequest(
            Long customerId,
            java.time.LocalDate saleDate,
            @Size(max = 500) String notes,
            Boolean issue,
            @NotEmpty(message = "La venta debe tener al menos un producto") @Valid List<ItemRequest> items,
            BigDecimal initialPaymentAmount,
            PaymentMethod initialPaymentMethod) {
    }

    public record IssueRequest(@Size(max = 500) String notes) {
    }

    public record VoidRequest(@NotNull(message = "El motivo es obligatorio") @Size(max = 255) String reason) {
    }

    public record ItemResponse(
            Long id,
            Long productId,
            String sku,
            String productName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal taxRate,
            BigDecimal lineSubtotal,
            BigDecimal lineDiscount,
            BigDecimal lineTax,
            BigDecimal lineTotal,
            BigDecimal unitCost,
            BigDecimal lineProfit) {
    }

    public record Response(
            Long id,
            String number,
            Long customerId,
            String customerIdentification,
            String customerName,
            String customerAddress,
            String customerEmail,
            SaleStatus status,
            PaymentStatus paymentStatus,
            LocalDate saleDate,
            Instant issuedAt,
            Instant voidedAt,
            String voidReason,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal balance,
            String notes,
            Long createdBy,
            String createdByName,
            Instant createdAt,
            List<ItemResponse> items,
            List<PaymentDtos.Response> payments) {
    }
}
