package com.facturapro.purchases;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.common.util.Money;
import com.facturapro.common.web.PageResponse;
import com.facturapro.inventory.MovementType;
import com.facturapro.inventory.StockMovementRepository;
import com.facturapro.inventory.StockService;
import com.facturapro.invoices.DocumentNumberService;
import com.facturapro.products.Product;
import com.facturapro.products.ProductRepository;
import com.facturapro.sales.PricingService;
import com.facturapro.security.CurrentUser;
import com.facturapro.settings.SettingsService;
import com.facturapro.suppliers.Supplier;
import com.facturapro.suppliers.SupplierRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compras a proveedores. Las compras en borrador NO modifican existencias;
 * el inventario se actualiza solo al confirmar, dentro de una transaccion.
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final PurchaseRepository purchaseRepository;
    private final PurchaseReturnRepository returnRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final PricingService pricingService;
    private final StockService stockService;
    private final DocumentNumberService documentNumberService;
    private final SettingsService settingsService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    @Transactional
    public PurchaseDtos.Response create(PurchaseDtos.Request request) {
        Purchase purchase = new Purchase();
        purchase.setNumber(documentNumberService.nextPurchaseNumber());
        purchase.setSupplier(findSupplier(request.supplierId()));
        purchase.setPurchaseDate(request.purchaseDate() == null ? LocalDate.now(ECUADOR) : request.purchaseDate());
        purchase.setNotes(request.notes());
        purchase.setStatus(PurchaseStatus.DRAFT);
        purchase.setCreatedBy(currentUser.id());
        purchase.setCreatedByName(currentUser.require().getFullName());
        buildItems(purchase, request.items());
        Purchase saved = purchaseRepository.save(purchase);
        auditService.record("PURCHASE_CREATED", "PURCHASE", saved.getId(), "Compra " + saved.getNumber());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseDtos.Response update(Long id, PurchaseDtos.Request request) {
        Purchase purchase = find(id);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("VALIDATION_ERROR", "Solo las compras en borrador pueden editarse.");
        }
        purchase.setSupplier(findSupplier(request.supplierId()));
        if (request.purchaseDate() != null) {
            purchase.setPurchaseDate(request.purchaseDate());
        }
        purchase.setNotes(request.notes());
        buildItems(purchase, request.items());
        Purchase saved = purchaseRepository.save(purchase);
        auditService.record("PURCHASE_UPDATED", "PURCHASE", saved.getId(), "Compra " + saved.getNumber());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseDtos.Response confirm(Long id) {
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compra no encontrada."));
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("VALIDATION_ERROR", "Solo las compras en borrador pueden confirmarse.");
        }
        for (PurchaseItem item : purchase.getItems()) {
            stockService.apply(item.getProduct().getId(), MovementType.PURCHASE, item.getQuantity(),
                    item.getUnitCost(), "PURCHASE", purchase.getId(), "Entrada por compra " + purchase.getNumber());
            Product product = productRepository.findById(item.getProduct().getId()).orElseThrow();
            product.setPurchasePrice(item.getUnitCost());
            productRepository.save(product);
        }
        purchase.setStatus(PurchaseStatus.CONFIRMED);
        purchase.setConfirmedAt(Instant.now());
        Purchase saved = purchaseRepository.save(purchase);
        auditService.record("PURCHASE_CONFIRMED", "PURCHASE", saved.getId(),
                "Compra confirmada " + saved.getNumber() + " por " + saved.getTotal());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseDtos.Response cancel(Long id, String reason) {
        Purchase purchase = find(id);
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Solo se pueden cancelar compras en borrador. Para devolver mercancia use una devolucion.");
        }
        purchase.setStatus(PurchaseStatus.CANCELLED);
        purchase.setNotes(reason);
        Purchase saved = purchaseRepository.save(purchase);
        auditService.record("PURCHASE_CANCELLED", "PURCHASE", saved.getId(),
                "Compra " + saved.getNumber() + " cancelada. Motivo: " + reason);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseDtos.ReturnResponse returnItems(Long id, PurchaseDtos.ReturnRequest request) {
        Purchase purchase = find(id);
        if (purchase.getStatus() != PurchaseStatus.CONFIRMED) {
            throw new BusinessException("VALIDATION_ERROR", "Solo se pueden devolver mercancia de compras confirmadas.");
        }
        Map<Long, PurchaseItem> byProduct = new HashMap<>();
        for (PurchaseItem item : purchase.getItems()) {
            byProduct.put(item.getProduct().getId(), item);
        }
        PurchaseReturn returnRecord = new PurchaseReturn();
        returnRecord.setNumber(documentNumberService.nextPurchaseReturnNumber());
        returnRecord.setPurchase(purchase);
        returnRecord.setReturnDate(LocalDate.now(ECUADOR));
        returnRecord.setReason(request.reason().trim());
        returnRecord.setCreatedBy(currentUser.id());
        returnRecord.setCreatedByName(currentUser.require().getFullName());

        BigDecimal total = Money.ZERO;
        for (PurchaseDtos.ReturnItemRequest itemRequest : request.items()) {
            PurchaseItem purchased = byProduct.get(itemRequest.productId());
            if (purchased == null) {
                throw new BusinessException("VALIDATION_ERROR", "El producto no pertenece a esta compra.");
            }
            BigDecimal alreadyReturned = returnedQuantity(purchase.getId(), itemRequest.productId());
            BigDecimal maxReturnable = purchased.getQuantity().subtract(alreadyReturned);
            if (itemRequest.quantity().compareTo(maxReturnable) > 0) {
                throw new BusinessException("VALIDATION_ERROR",
                        "La cantidad a devolver supera lo recibido (maximo " + maxReturnable + ").");
            }
            stockService.apply(itemRequest.productId(), MovementType.PURCHASE_RETURN,
                    itemRequest.quantity().negate(), purchased.getUnitCost(), "PURCHASE_RETURN",
                    purchase.getId(), "Devolucion de compra " + purchase.getNumber());

            PurchaseReturnItem item = new PurchaseReturnItem();
            item.setProduct(purchased.getProduct());
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(purchased.getUnitCost());
            item.setLineTotal(Money.round(purchased.getUnitCost().multiply(itemRequest.quantity())));
            returnRecord.addItem(item);
            total = total.add(item.getLineTotal());
        }
        returnRecord.setTotal(total);
        PurchaseReturn saved = returnRepository.save(returnRecord);
        auditService.record("PURCHASE_RETURNED", "PURCHASE", purchase.getId(),
                "Devolucion " + saved.getNumber() + " sobre compra " + purchase.getNumber());
        return new PurchaseDtos.ReturnResponse(
                saved.getId(), saved.getNumber(), purchase.getId(), purchase.getNumber(),
                saved.getReturnDate(), saved.getReason(), saved.getTotal(),
                saved.getItems().stream()
                        .map(i -> new PurchaseDtos.ReturnItemResponse(i.getProduct().getId(),
                                i.getProduct().getSku(), i.getProduct().getName(),
                                i.getQuantity(), i.getUnitCost(), i.getLineTotal()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseDtos.Response> list(PurchaseStatus status, Long supplierId,
                                                     LocalDate from, LocalDate to, int page, int size) {
        Specification<Purchase> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Purchase> result = purchaseRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseDtos.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Purchase find(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compra no encontrada."));
    }

    private BigDecimal returnedQuantity(Long purchaseId, Long productId) {
        return movementRepository.sumReturnedByPurchaseAndProduct(purchaseId, productId);
    }

    private void buildItems(Purchase purchase, List<PurchaseDtos.ItemRequest> items) {
        purchase.getItems().clear();
        List<PricingService.LineCalculation> calculations = new ArrayList<>();
        for (PurchaseDtos.ItemRequest itemRequest : items) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado."));
            BigDecimal taxRate = settingsService.getTaxRate(product.getTaxCategory().name());
            BigDecimal discount = itemRequest.discountPercent() == null ? BigDecimal.ZERO : itemRequest.discountPercent();
            PricingService.LineCalculation calc = pricingService.calculateLine(
                    itemRequest.quantity(), itemRequest.unitCost(), discount, taxRate);
            calculations.add(calc);

            PurchaseItem item = new PurchaseItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(itemRequest.unitCost());
            item.setDiscountPercent(discount);
            item.setTaxRate(taxRate);
            item.setLineSubtotal(calc.subtotal());
            item.setLineDiscount(calc.discount());
            item.setLineTax(calc.tax());
            item.setLineTotal(calc.total());
            purchase.addItem(item);
        }
        PricingService.DocumentTotals totals = pricingService.totals(calculations);
        purchase.setSubtotal(totals.subtotal());
        purchase.setDiscountTotal(totals.discountTotal());
        purchase.setTaxTotal(totals.taxTotal());
        purchase.setTotal(totals.total());
    }

    private Supplier findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "El proveedor indicado no existe."));
    }

    private PurchaseDtos.Response toResponse(Purchase purchase) {
        List<PurchaseDtos.ItemResponse> items = purchase.getItems().stream()
                .map(i -> new PurchaseDtos.ItemResponse(
                        i.getId(), i.getProduct().getId(), i.getProduct().getSku(), i.getProduct().getName(),
                        i.getQuantity(), i.getUnitCost(), i.getDiscountPercent(), i.getTaxRate(),
                        i.getLineSubtotal(), i.getLineDiscount(), i.getLineTax(), i.getLineTotal()))
                .toList();
        return new PurchaseDtos.Response(
                purchase.getId(), purchase.getNumber(),
                purchase.getSupplier().getId(), purchase.getSupplier().getBusinessName(),
                purchase.getSupplier().getIdentification(),
                purchase.getPurchaseDate(), purchase.getStatus(),
                purchase.getSubtotal(), purchase.getDiscountTotal(), purchase.getTaxTotal(), purchase.getTotal(),
                purchase.getNotes(), purchase.getConfirmedAt(),
                purchase.getCreatedBy(), purchase.getCreatedByName(), purchase.getCreatedAt(),
                items);
    }
}
