package com.facturapro.payments;

import com.facturapro.sales.Sale;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySaleIdOrderByPaymentDateDesc(Long saleId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsBySaleIdAndVoidedFalse(Long saleId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.sale.id = :saleId and p.voided = false")
    java.math.BigDecimal sumActiveBySale(@Param("saleId") Long saleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :saleId")
    Optional<Sale> findSaleForUpdate(@Param("saleId") Long saleId);
}
