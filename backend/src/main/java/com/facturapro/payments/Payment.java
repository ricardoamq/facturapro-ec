package com.facturapro.payments;

import com.facturapro.sales.PaymentMethod;
import com.facturapro.sales.Sale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Abono registrado sobre una venta. El historial nunca se borra:
 * las anulaciones marcan el pago como voided y el saldo se recalcula.
 */
@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", foreignKey = @ForeignKey(name = "fk_payments_sale"))
    private Sale sale;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 60)
    private String reference;

    @Column(length = 255)
    private String notes;

    @Column(name = "payment_date", nullable = false)
    private Instant paymentDate = Instant.now();

    @Column(nullable = false)
    private boolean voided;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_by_name", length = 120)
    private String createdByName;
}
