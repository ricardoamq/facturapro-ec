package com.facturapro.inventory;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.products.Product;
import com.facturapro.products.ProductRepository;
import com.facturapro.security.CurrentUser;
import com.facturapro.settings.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Unica puerta de entrada para modificar existencias.
 * Todo cambio de stock genera un movimiento de inventario trazable
 * (usuario, fecha, motivo y referencia).
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final SettingsService settingsService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    @Transactional
    public StockMovement apply(Long productId, MovementType type, BigDecimal signedQuantity,
                               BigDecimal unitCost, String referenceType, Long referenceId, String reason) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado."));
        BigDecimal newStock = product.getStock().add(signedQuantity);
        boolean allowNegative = settingsService.getBoolean("inventory.allowNegativeStock");
        if (newStock.signum() < 0 && !allowNegative) {
            throw new BusinessException("INSUFFICIENT_STOCK",
                    "Stock insuficiente para '" + product.getName() + "': disponible "
                            + product.getStock() + ", solicitado " + signedQuantity.abs() + ".");
        }
        product.setStock(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantity(signedQuantity);
        movement.setBalanceAfter(newStock);
        movement.setUnitCost(unitCost);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setReason(reason);
        movement.setUserId(currentUser.id());
        movement.setUsername(currentUser.username());
        return movementRepository.save(movement);
    }

    @Transactional
    public StockMovement adjust(Long productId, BigDecimal quantity, String reason) {
        if (quantity == null || quantity.signum() == 0) {
            throw new BusinessException("VALIDATION_ERROR", "La cantidad del ajuste no puede ser cero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("VALIDATION_ERROR", "El motivo del ajuste es obligatorio.");
        }
        StockMovement movement = apply(productId, MovementType.ADJUSTMENT, quantity, null,
                "ADJUSTMENT", null, reason.trim());
        auditService.record("STOCK_ADJUSTED", "PRODUCT", productId,
                "Ajuste de " + quantity + " (" + reason + ")");
        return movement;
    }

    @Transactional(readOnly = true)
    public boolean hasMovements(Long productId) {
        return movementRepository.existsByProductId(productId);
    }
}
