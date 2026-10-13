package com.facturapro.purchases;

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

@Getter
@Setter
@Entity
@Table(name = "purchase_items")
public class PurchaseItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", foreignKey = @ForeignKey(name = "fk_pi_purchase"))
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_pi_product"))
    private com.facturapro.products.Product product;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

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
}
