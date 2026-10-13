package com.facturapro.products;

import com.facturapro.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    @Column(name = "purchase_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "sale_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String unit = "UND";

    @Column(length = 60)
    private String barcode;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(name = "min_stock", nullable = false, precision = 14, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_category", nullable = false, length = 20)
    private TaxCategory taxCategory = TaxCategory.GENERAL;

    @Column(nullable = false)
    private boolean active = true;
}
