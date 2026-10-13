package com.facturapro.purchases;

import com.facturapro.common.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASES_VIEW')")
    public PageResponse<PurchaseDtos.Response> list(
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return purchaseService.list(status, supplierId, from, to,
                Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASES_VIEW')")
    public PurchaseDtos.Response get(@PathVariable Long id) {
        return purchaseService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASES_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDtos.Response create(@Valid @RequestBody PurchaseDtos.Request request) {
        return purchaseService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASES_WRITE')")
    public PurchaseDtos.Response update(@PathVariable Long id, @Valid @RequestBody PurchaseDtos.Request request) {
        return purchaseService.update(id, request);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('PURCHASES_WRITE')")
    public PurchaseDtos.Response confirm(@PathVariable Long id) {
        return purchaseService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASES_WRITE')")
    public PurchaseDtos.Response cancel(@PathVariable Long id,
                                        @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return purchaseService.cancel(id, reason == null || reason.isBlank() ? "Sin motivo" : reason);
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize("hasAuthority('PURCHASES_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDtos.ReturnResponse returnItems(@PathVariable Long id,
                                                   @Valid @RequestBody PurchaseDtos.ReturnRequest request) {
        return purchaseService.returnItems(id, request);
    }
}
