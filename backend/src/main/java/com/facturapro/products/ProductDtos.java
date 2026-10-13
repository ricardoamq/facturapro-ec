package com.facturapro.products;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDtos() {

    public record CategoryRequest(
            @NotBlank(message = "El nombre de la categoria es obligatorio") @Size(max = 80) String name,
            @Size(max = 255) String description,
            Boolean active) {
    }

    public record CategoryResponse(Long id, String name, String description, boolean active) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.isActive());
        }
    }

    public record CreateRequest(
            @NotBlank(message = "El SKU es obligatorio") @Size(max = 40) String sku,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
            @Size(max = 500) String description,
            Long categoryId,
            @NotNull(message = "El precio de compra es obligatorio")
            @DecimalMin(value = "0", message = "El precio de compra no puede ser negativo") BigDecimal purchasePrice,
            @NotNull(message = "El precio de venta es obligatorio")
            @DecimalMin(value = "0", message = "El precio de venta no puede ser negativo") BigDecimal salePrice,
            @NotBlank(message = "La unidad de medida es obligatoria") @Size(max = 20) String unit,
            @Size(max = 60) String barcode,
            @NotNull(message = "La existencia inicial es obligatoria")
            @DecimalMin(value = "0", message = "La existencia inicial no puede ser negativa") BigDecimal initialStock,
            @NotNull(message = "El stock minimo es obligatorio")
            @DecimalMin(value = "0", message = "El stock minimo no puede ser negativo") BigDecimal minStock,
            TaxCategory taxCategory,
            Boolean active) {
    }

    public record UpdateRequest(
            @NotBlank(message = "El SKU es obligatorio") @Size(max = 40) String sku,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
            @Size(max = 500) String description,
            Long categoryId,
            @NotNull @DecimalMin("0") BigDecimal purchasePrice,
            @NotNull @DecimalMin("0") BigDecimal salePrice,
            @NotBlank @Size(max = 20) String unit,
            @Size(max = 60) String barcode,
            @NotNull @DecimalMin("0") BigDecimal minStock,
            TaxCategory taxCategory,
            Boolean active) {
    }

    public record Response(
            Long id,
            String sku,
            String name,
            String description,
            Long categoryId,
            String categoryName,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            BigDecimal margin,
            BigDecimal marginPercent,
            String unit,
            String barcode,
            BigDecimal stock,
            BigDecimal minStock,
            TaxCategory taxCategory,
            BigDecimal taxRate,
            boolean lowStock,
            boolean active,
            Instant createdAt) {

        public static Response from(Product p, BigDecimal taxRate) {
            BigDecimal margin = p.getSalePrice().subtract(p.getPurchasePrice());
            BigDecimal marginPercent = p.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0
                    ? margin.multiply(new BigDecimal("100")).divide(p.getPurchasePrice(), 2, java.math.RoundingMode.HALF_UP)
                    : (margin.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.00") : BigDecimal.ZERO.setScale(2));
            return new Response(
                    p.getId(), p.getSku(), p.getName(), p.getDescription(),
                    p.getCategory() == null ? null : p.getCategory().getId(),
                    p.getCategory() == null ? null : p.getCategory().getName(),
                    p.getPurchasePrice(), p.getSalePrice(), margin, marginPercent,
                    p.getUnit(), p.getBarcode(), p.getStock(), p.getMinStock(),
                    p.getTaxCategory(), taxRate,
                    p.getStock().compareTo(p.getMinStock()) <= 0,
                    p.isActive(), p.getCreatedAt());
        }
    }

    public record StockAdjustRequest(
            @NotNull(message = "La cantidad es obligatoria") BigDecimal quantity,
            @Size(max = 255) String reason) {
    }
}
