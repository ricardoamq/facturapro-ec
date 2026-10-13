package com.facturapro.sales;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    Optional<Sale> findByIdempotencyKey(String idempotencyKey);

    Optional<Sale> findByNumber(String number);

    /**
     * Trae la venta con los renglones ya inicializados para poder serializarla o
     * generar el PDF fuera de la transaccion (open-in-view deshabilitado).
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "items")
    Optional<Sale> findWithItemsByNumber(String number);

    boolean existsByNumber(String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> findByIdForUpdate(@Param("id") Long id);

    @Query("select count(s) from Sale s where s.status = com.facturapro.sales.SaleStatus.ISSUED")
    long countIssued();

    @Query("select coalesce(sum(s.total), 0) from Sale s where s.status = com.facturapro.sales.SaleStatus.ISSUED")
    java.math.BigDecimal sumIssuedTotal();
}
