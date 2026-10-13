package com.facturapro.inventory;

import com.facturapro.common.web.PageResponse;
import com.facturapro.products.ProductDtos;
import com.facturapro.products.ProductRepository;
import com.facturapro.settings.SettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final StockService stockService;
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final SettingsService settingsService;

    public record AdjustmentRequest(
            @NotNull(message = "El producto es obligatorio") Long productId,
            @NotNull(message = "La cantidad es obligatoria") BigDecimal quantity,
            @NotBlank(message = "El motivo es obligatorio") @Size(max = 255) String reason) {
    }

    public record MovementResponse(
            Long id,
            Long productId,
            String productSku,
            String productName,
            String movementType,
            BigDecimal quantity,
            BigDecimal balanceAfter,
            BigDecimal unitCost,
            String referenceType,
            Long referenceId,
            String reason,
            String username,
            Instant createdAt) {

        public static MovementResponse from(StockMovement m) {
            return new MovementResponse(
                    m.getId(), m.getProduct().getId(), m.getProduct().getSku(), m.getProduct().getName(),
                    m.getMovementType().name(), m.getQuantity(), m.getBalanceAfter(), m.getUnitCost(),
                    m.getReferenceType(), m.getReferenceId(), m.getReason(), m.getUsername(), m.getCreatedAt());
        }
    }

    public record InventorySummary(long activeProducts, long lowStockProducts,
                                   BigDecimal stockValue, boolean allowNegativeStock) {
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public PageResponse<MovementResponse> movements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Specification<StockMovement> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("movementType"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(ECUADOR).toInstant()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(ECUADOR).toInstant()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        var result = movementRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, MovementResponse::from);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public List<ProductDtos.Response> lowStock() {
        return productRepository.findLowStock().stream()
                .map(p -> ProductDtos.Response.from(p, settingsService.getTaxRate(p.getTaxCategory().name())))
                .toList();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public InventorySummary summary() {
        return new InventorySummary(
                productRepository.countByActiveTrue(),
                productRepository.findLowStock().size(),
                productRepository.totalStockValue(),
                settingsService.getBoolean("inventory.allowNegativeStock"));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public MovementResponse adjust(@Valid @RequestBody AdjustmentRequest request) {
        StockMovement movement = stockService.adjust(request.productId(), request.quantity(), request.reason());
        return MovementResponse.from(movement);
    }
}
