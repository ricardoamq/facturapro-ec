package com.facturapro.sales;

import com.facturapro.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Linea de venta con instantanea de los datos del producto al momento de emitir.
 * Los cambios posteriores del catalogo no alteran los documentos historicos.
 */
@Getter
@Setter
@Entity
@Table(name = "sale_items")
public class SaleItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", foreignKey = @ForeignKey(name = "fk_si_sale"))
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_si_product"))
    private com.facturapro.products.Product product;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", nullable = false, precision = 7, scale = 4)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineSubtotal;

    @Column(name = "line_discount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineDiscount = BigDecimal.ZERO;

    @Column(name = "line_tax", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTax = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    /** Costo de compra al momento de la venta: permite calcular utilidad estimada. */
    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;
}
