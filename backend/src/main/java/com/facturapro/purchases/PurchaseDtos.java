package com.facturapro.purchases;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseDtos() {

    public record ItemRequest(
            @NotNull(message = "Indique el producto") Long productId,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero") BigDecimal quantity,
            @NotNull(message = "El costo unitario es obligatorio")
            @DecimalMin(value = "0", message = "El costo no puede ser negativo") BigDecimal unitCost,
            BigDecimal discountPercent) {
    }

    public record Request(
            @NotNull(message = "El proveedor es obligatorio") Long supplierId,
            LocalDate purchaseDate,
            @Size(max = 500) String notes,
            @NotEmpty(message = "La compra debe tener al menos un producto") @Valid List<ItemRequest> items) {
    }

    public record ItemResponse(
            Long id,
            Long productId,
            String sku,
            String productName,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal discountPercent,
            BigDecimal taxRate,
            BigDecimal lineSubtotal,
            BigDecimal lineDiscount,
            BigDecimal lineTax,
            BigDecimal lineTotal) {
    }

    public record Response(
            Long id,
            String number,
            Long supplierId,
            String supplierName,
            String supplierIdentification,
            LocalDate purchaseDate,
            PurchaseStatus status,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal total,
            String notes,
            Instant confirmedAt,
            Long createdBy,
            String createdByName,
            Instant createdAt,
            List<ItemResponse> items) {
    }

    public record ReturnItemRequest(
            @NotNull Long productId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity) {
    }

    public record ReturnRequest(
            @NotNull(message = "El motivo es obligatorio") @Size(max = 255) String reason,
            @NotEmpty @Valid List<ReturnItemRequest> items) {
    }

    public record ReturnItemResponse(Long productId, String sku, String productName,
                                     BigDecimal quantity, BigDecimal unitCost, BigDecimal lineTotal) {
    }

    public record ReturnResponse(
            Long id,
            String number,
            Long purchaseId,
            String purchaseNumber,
            LocalDate returnDate,
            String reason,
            BigDecimal total,
            List<ReturnItemResponse> items) {
    }
}
