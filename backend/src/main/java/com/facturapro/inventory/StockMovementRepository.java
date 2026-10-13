package com.facturapro.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    /**
     * Carga el producto en la misma consulta (join fetch) para poder serializar
     * los movimientos fuera de la transaccion (open-in-view deshabilitado).
     */
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "product")
    Page<StockMovement> findAll(org.springframework.data.jpa.domain.Specification<StockMovement> spec, Pageable pageable);

    boolean existsByProductId(Long productId);

    @Query("select m from StockMovement m where m.product.id = :productId order by m.createdAt desc")
    List<StockMovement> findRecentByProduct(@Param("productId") Long productId, Pageable pageable);

    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m where m.product.id = :productId")
    BigDecimal sumQuantityByProduct(@Param("productId") Long productId);

    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select coalesce(sum(abs(m.quantity)), 0) from StockMovement m "
            + "where m.movementType = com.facturapro.inventory.MovementType.PURCHASE_RETURN "
            + "and m.referenceId = :referenceId and m.product.id = :productId")
    BigDecimal sumReturnedByPurchaseAndProduct(@Param("referenceId") Long referenceId,
                                               @Param("productId") Long productId);
}
