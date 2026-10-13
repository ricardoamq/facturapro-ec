package com.facturapro.sales;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.common.util.Money;
import com.facturapro.common.web.PageResponse;
import com.facturapro.customers.Customer;
import com.facturapro.customers.CustomerRepository;
import com.facturapro.inventory.MovementType;
import com.facturapro.inventory.StockService;
import com.facturapro.invoices.DocumentNumberService;
import com.facturapro.payments.PaymentDtos;
import com.facturapro.payments.PaymentService;
import com.facturapro.products.Product;
import com.facturapro.products.ProductRepository;
import com.facturapro.security.CurrentUser;
import com.facturapro.settings.SettingsService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    public static final String FINAL_CONSUMER_ID = "9999999999999";
    public static final String FINAL_CONSUMER_NAME = "CONSUMIDOR FINAL";

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final int MAX_ITEMS = 200;

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    private final StockService stockService;
    private final DocumentNumberService documentNumberService;
    private final SettingsService settingsService;
    private final PaymentService paymentService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    @Transactional
    public SaleDtos.Response create(SaleDtos.CreateRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = saleRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        boolean issue = request.issue() == null || request.issue();
        if (!issue && !settingsService.getBoolean("sales.allowDrafts")) {
            throw new BusinessException("VALIDATION_ERROR", "Los borradores de venta estan deshabilitados.");
        }
        if (request.items().size() > MAX_ITEMS) {
            throw new BusinessException("VALIDATION_ERROR", "La venta no puede tener mas de " + MAX_ITEMS + " lineas.");
        }
        Sale sale = new Sale();
        sale.setSaleDate(request.saleDate() == null ? LocalDate.now(ECUADOR) : request.saleDate());
        sale.setStatus(issue ? SaleStatus.ISSUED : SaleStatus.DRAFT);
        sale.setNotes(request.notes());
        sale.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim());
        sale.setCreatedBy(currentUser.id());
        sale.setCreatedByName(currentUser.require().getFullName());
        applyCustomer(sale, request.customerId());
        buildItems(sale, request.items(), issue);
        applyTotals(sale);
        if (issue) {
            sale.setNumber(documentNumberService.nextSaleNumber());
            sale.setIssuedAt(Instant.now());
        }
        Sale saved;
        try {
            saved = saleRepository.saveAndFlush(sale);
        } catch (DataIntegrityViolationException ex) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                return saleRepository.findByIdempotencyKey(idempotencyKey.trim())
                        .map(this::toResponse)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (issue) {
            deductStock(saved);
        }
        if (issue && request.initialPaymentAmount() != null && request.initialPaymentAmount().signum() > 0) {
            PaymentDtos.RegisterRequest payment = new PaymentDtos.RegisterRequest(
                    request.initialPaymentAmount(),
                    request.initialPaymentMethod() == null ? PaymentMethod.CASH : request.initialPaymentMethod(),
                    null, "Pago inicial al emitir");
            paymentService.register(saved.getId(), payment, null);
        }
        auditService.record(issue ? "SALE_ISSUED" : "SALE_DRAFT_CREATED", "SALE", saved.getId(),
                (issue ? "Venta emitida " : "Borrador ") + (saved.getNumber() == null ? "#" + saved.getId() : saved.getNumber())
                        + " por " + saved.getTotal());
        return toResponse(find(saved.getId()));
    }

    @Transactional
    public SaleDtos.Response issue(Long id, SaleDtos.IssueRequest request) {
        Sale sale = saleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Venta no encontrada."));
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BusinessException("VALIDATION_ERROR", "Solo las ventas en borrador pueden emitirse.");
        }
        buildItems(sale, sale.getItems().stream().map(i -> new SaleDtos.ItemRequest(
                i.getProduct() == null ? null : i.getProduct().getId(),
                i.getQuantity(), i.getUnitPrice(), i.getDiscountPercent())).toList(), true);
        applyTotals(sale);
        sale.setStatus(SaleStatus.ISSUED);
        sale.setNumber(documentNumberService.nextSaleNumber());
        sale.setIssuedAt(Instant.now());
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            sale.setNotes(request.notes());
        }
        Sale saved = saleRepository.saveAndFlush(sale);
        deductStock(saved);
        auditService.record("SALE_ISSUED", "SALE", saved.getId(), "Venta emitida " + saved.getNumber());
        return toResponse(saved);
    }

    /**
     * Anulacion controlada: exige motivo, revierte el inventario y registra auditoria.
     * No se permite anular una venta que tenga pagos activos; anule primero los pagos.
     */
    @Transactional
    public SaleDtos.Response voidSale(Long id, String reason) {
        Sale sale = saleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Venta no encontrada."));
        if (sale.getStatus() != SaleStatus.ISSUED) {
            throw new BusinessException("VALIDATION_ERROR", "Solo se pueden anular ventas emitidas.");
        }
        if (paymentService.hasActivePayments(id)) {
            throw new BusinessException("VALIDATION_ERROR",
                    "La venta tiene pagos registrados. Anule los pagos antes de anular la venta.");
        }
        for (SaleItem item : sale.getItems()) {
            if (item.getProduct() != null) {
                stockService.apply(item.getProduct().getId(), MovementType.SALE_RETURN, item.getQuantity(),
                        item.getUnitCost(), "SALE", sale.getId(), "Anulacion de venta " + sale.getNumber());
            }
        }
        sale.setStatus(SaleStatus.VOIDED);
        sale.setVoidedAt(Instant.now());
        sale.setVoidReason(reason);
        sale.setVoidedBy(currentUser.id());
        Sale saved = saleRepository.save(sale);
        auditService.record("SALE_VOIDED", "SALE", saved.getId(),
                "Venta " + saved.getNumber() + " anulada. Motivo: " + reason);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleDtos.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Sale find(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Venta no encontrada."));
    }

    @Transactional(readOnly = true)
    public SaleDtos.Response getByNumber(String number) {
        return saleRepository.findByNumber(number)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado."));
    }

    @Transactional(readOnly = true)
    public Sale byNumberEntity(String number) {
        return saleRepository.findWithItemsByNumber(number)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado."));
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleDtos.Response> list(String search, SaleStatus status, PaymentStatus paymentStatus,
                                                Long customerId, Long userId, LocalDate from, LocalDate to,
                                                int page, int size) {
        Specification<Sale> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("number")), like),
                        cb.like(cb.lower(root.get("customerName")), like),
                        cb.like(cb.lower(root.get("customerIdentification")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("createdBy"), userId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("saleDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("saleDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Sale> result = saleRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, this::toResponse);
    }

    @Transactional
    public PaymentDtos.Response addPayment(Long saleId, PaymentDtos.RegisterRequest request, String idempotencyKey) {
        PaymentDtos.Response response = PaymentDtos.Response.from(
                paymentService.register(saleId, request, idempotencyKey));
        return response;
    }

    private void deductStock(Sale sale) {
        for (SaleItem item : sale.getItems()) {
            if (item.getProduct() != null) {
                stockService.apply(item.getProduct().getId(), MovementType.SALE, item.getQuantity().negate(),
                        item.getUnitCost(), "SALE", sale.getId(), "Salida por venta " + sale.getNumber());
            }
        }
    }

    private void applyCustomer(Sale sale, Long customerId) {
        if (customerId == null) {
            sale.setCustomer(null);
            sale.setCustomerIdentification(FINAL_CONSUMER_ID);
            sale.setCustomerName(FINAL_CONSUMER_NAME);
            return;
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "El cliente indicado no existe."));
        if (!customer.isActive()) {
            throw new BusinessException("VALIDATION_ERROR", "El cliente indicado esta inactivo.");
        }
        sale.setCustomer(customer);
        sale.setCustomerIdentification(customer.getIdentification());
        sale.setCustomerName(customer.getBusinessName());
        sale.setCustomerAddress(customer.getAddress());
        sale.setCustomerEmail(customer.getEmail());
    }

    private void buildItems(Sale sale, List<SaleDtos.ItemRequest> items, boolean checkStock) {
        sale.getItems().clear();
        List<PricingService.LineCalculation> calculations = new ArrayList<>();
        for (SaleDtos.ItemRequest itemRequest : items) {
            Product product = loadProduct(itemRequest.productId(), checkStock);
            if (checkStock && product.getStock().compareTo(itemRequest.quantity()) < 0
                    && !settingsService.getBoolean("inventory.allowNegativeStock")) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                        "Stock insuficiente para '" + product.getName() + "': disponible "
                                + product.getStock() + ", solicitado " + itemRequest.quantity() + ".");
            }
            BigDecimal taxRate = settingsService.getTaxRate(product.getTaxCategory().name());
            BigDecimal unitPrice = itemRequest.unitPrice() == null ? product.getSalePrice() : itemRequest.unitPrice();
            BigDecimal discount = itemRequest.discountPercent() == null ? BigDecimal.ZERO : itemRequest.discountPercent();
            PricingService.LineCalculation calc = pricingService.calculateLine(
                    itemRequest.quantity(), unitPrice, discount, taxRate);
            calculations.add(calc);

            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setSku(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setDiscountPercent(discount);
            item.setTaxRate(taxRate);
            item.setLineSubtotal(calc.subtotal());
            item.setLineDiscount(calc.discount());
            item.setLineTax(calc.tax());
            item.setLineTotal(calc.total());
            item.setUnitCost(product.getPurchasePrice());
            sale.addItem(item);
        }
        sale.setSubtotal(Money.ZERO);
        PricingService.DocumentTotals totals = pricingService.totals(calculations);
        sale.setSubtotal(totals.subtotal());
        sale.setDiscountTotal(totals.discountTotal());
        sale.setTaxTotal(totals.taxTotal());
        sale.setTotal(totals.total());
    }

    private Product loadProduct(Long productId, boolean lock) {
        if (productId == null) {
            throw new BusinessException("VALIDATION_ERROR", "Cada linea de venta debe indicar un producto.");
        }
        Product product = lock
                ? productRepository.findByIdForUpdate(productId).orElse(null)
                : productRepository.findById(productId).orElse(null);
        if (product == null) {
            throw new NotFoundException("Producto no encontrado.");
        }
        if (!product.isActive()) {
            throw new BusinessException("VALIDATION_ERROR", "El producto '" + product.getName() + "' esta inactivo.");
        }
        return product;
    }

    private void applyTotals(Sale sale) {
        if (sale.getItems().isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "La venta debe tener al menos un producto.");
        }
        List<PricingService.LineCalculation> calculations = sale.getItems().stream()
                .map(i -> new PricingService.LineCalculation(i.getLineSubtotal(), i.getLineDiscount(),
                        i.getLineTax(), i.getLineTotal()))
                .toList();
        PricingService.DocumentTotals totals = pricingService.totals(calculations);
        sale.setSubtotal(totals.subtotal());
        sale.setDiscountTotal(totals.discountTotal());
        sale.setTaxTotal(totals.taxTotal());
        sale.setTotal(totals.total());
        if (sale.getAmountPaid() == null || sale.getAmountPaid().signum() == 0) {
            sale.setBalance(totals.total());
            sale.setPaymentStatus(PaymentStatus.PENDING);
        } else {
            sale.setBalance(Money.round(totals.total().subtract(sale.getAmountPaid())));
            sale.setPaymentStatus(sale.getBalance().signum() <= 0 ? PaymentStatus.PAID : PaymentStatus.PARTIAL);
        }
    }

    private SaleDtos.Response toResponse(Sale sale) {
        List<SaleDtos.ItemResponse> items = sale.getItems().stream()
                .map(i -> new SaleDtos.ItemResponse(
                        i.getId(),
                        i.getProduct() == null ? null : i.getProduct().getId(),
                        i.getSku(), i.getProductName(), i.getQuantity(), i.getUnitPrice(),
                        i.getDiscountPercent(), i.getTaxRate(), i.getLineSubtotal(), i.getLineDiscount(),
                        i.getLineTax(), i.getLineTotal(), i.getUnitCost(),
                        Money.round(i.getLineSubtotal().subtract(i.getQuantity().multiply(i.getUnitCost())))))
                .toList();
        List<PaymentDtos.Response> payments = sale.getStatus() == SaleStatus.DRAFT
                ? List.of()
                : paymentService.paymentsOf(sale.getId()).stream().map(PaymentDtos.Response::from).toList();
        return new SaleDtos.Response(
                sale.getId(), sale.getNumber(),
                sale.getCustomer() == null ? null : sale.getCustomer().getId(),
                sale.getCustomerIdentification(), sale.getCustomerName(), sale.getCustomerAddress(),
                sale.getCustomerEmail(), sale.getStatus(), sale.getPaymentStatus(), sale.getSaleDate(),
                sale.getIssuedAt(), sale.getVoidedAt(), sale.getVoidReason(),
                sale.getSubtotal(), sale.getDiscountTotal(), sale.getTaxTotal(), sale.getTotal(),
                sale.getAmountPaid(), sale.getBalance(), sale.getNotes(),
                sale.getCreatedBy(), sale.getCreatedByName(), sale.getCreatedAt(),
                items, payments);
    }
}
