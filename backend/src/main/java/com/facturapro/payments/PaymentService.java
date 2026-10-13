package com.facturapro.payments;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.common.util.Money;
import com.facturapro.sales.PaymentStatus;
import com.facturapro.sales.Sale;
import com.facturapro.sales.SaleRepository;
import com.facturapro.sales.SaleStatus;
import com.facturapro.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    @Transactional
    public Payment register(Long saleId, PaymentDtos.RegisterRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Sale sale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new NotFoundException("Venta no encontrada."));
        if (sale.getStatus() != SaleStatus.ISSUED) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Solo se pueden registrar pagos sobre ventas emitidas.");
        }
        if (request.amount().compareTo(sale.getBalance()) > 0) {
            throw new BusinessException("VALIDATION_ERROR",
                    "El monto del pago (" + request.amount() + ") supera el saldo pendiente (" + sale.getBalance() + ").");
        }
        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setAmount(Money.round(request.amount()));
        payment.setPaymentMethod(request.method());
        payment.setReference(request.reference());
        payment.setNotes(request.notes());
        payment.setPaymentDate(Instant.now());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedBy(currentUser.id());
        payment.setCreatedByName(currentUser.require().getFullName());
        Payment saved = paymentRepository.save(payment);
        recomputeSaleBalance(sale);
        auditService.record("PAYMENT_REGISTERED", "PAYMENT", saved.getId(),
                "Pago de " + saved.getAmount() + " sobre venta " + sale.getNumber());
        return saved;
    }

    @Transactional
    public Payment voidPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado."));
        if (payment.isVoided()) {
            throw new BusinessException("VALIDATION_ERROR", "El pago ya fue anulado.");
        }
        payment.setVoided(true);
        payment.setVoidReason(reason);
        payment.setVoidedAt(Instant.now());
        payment.setVoidedBy(currentUser.id());
        paymentRepository.save(payment);
        Sale sale = payment.getSale();
        recomputeSaleBalance(sale);
        auditService.record("PAYMENT_VOIDED", "PAYMENT", payment.getId(),
                "Anulacion de pago de " + payment.getAmount() + " sobre venta " + sale.getNumber() + ". Motivo: " + reason);
        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> paymentsOf(Long saleId) {
        return paymentRepository.findBySaleIdOrderByPaymentDateDesc(saleId);
    }

    @Transactional(readOnly = true)
    public boolean hasActivePayments(Long saleId) {
        return paymentRepository.existsBySaleIdAndVoidedFalse(saleId);
    }

    private void recomputeSaleBalance(Sale sale) {
        BigDecimal paid = paymentRepository.sumActiveBySale(sale.getId());
        sale.setAmountPaid(paid);
        sale.setBalance(Money.round(sale.getTotal().subtract(paid)));
        if (sale.getBalance().signum() <= 0) {
            sale.setPaymentStatus(PaymentStatus.PAID);
            sale.setBalance(Money.ZERO);
        } else if (paid.signum() > 0) {
            sale.setPaymentStatus(PaymentStatus.PARTIAL);
        } else {
            sale.setPaymentStatus(PaymentStatus.PENDING);
        }
        saleRepository.save(sale);
    }
}
