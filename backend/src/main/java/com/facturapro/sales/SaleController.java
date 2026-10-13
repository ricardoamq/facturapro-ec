package com.facturapro.sales;

import com.facturapro.common.web.PageResponse;
import com.facturapro.payments.PaymentDtos;
import com.facturapro.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasAuthority('SALES_VIEW')")
    public PageResponse<SaleDtos.Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return saleService.list(search, status, paymentStatus, customerId, userId, from, to,
                Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALES_VIEW')")
    public SaleDtos.Response get(@PathVariable Long id) {
        return saleService.get(id);
    }

    @GetMapping("/by-number/{number}")
    @PreAuthorize("hasAuthority('SALES_VIEW')")
    public SaleDtos.Response byNumber(@PathVariable String number) {
        return saleService.getByNumber(number);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALES_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleDtos.Response create(
            @Valid @RequestBody SaleDtos.CreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return saleService.create(request, idempotencyKey);
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('SALES_WRITE')")
    public SaleDtos.Response issue(@PathVariable Long id,
                                   @RequestBody(required = false) SaleDtos.IssueRequest request) {
        return saleService.issue(id, request == null ? new SaleDtos.IssueRequest(null) : request);
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('SALES_VOID')")
    public SaleDtos.Response voidSale(@PathVariable Long id, @Valid @RequestBody SaleDtos.VoidRequest request) {
        return saleService.voidSale(id, request.reason());
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('PAYMENTS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDtos.Response pay(
            @PathVariable Long id,
            @Valid @RequestBody PaymentDtos.RegisterRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return saleService.addPayment(id, request, idempotencyKey);
    }

    @GetMapping("/meta/methods")
    public Map<String, String> paymentMethods() {
        return java.util.Arrays.stream(PaymentMethod.values())
                .collect(java.util.stream.Collectors.toMap(Enum::name, PaymentMethod::label));
    }

    @GetMapping("/permissions")
    public Map<String, Boolean> permissions() {
        var user = currentUser.require();
        return Map.of(
                "canVoid", user.getRole().has(com.facturapro.users.Permission.SALES_VOID),
                "canPay", user.getRole().has(com.facturapro.users.Permission.PAYMENTS_WRITE));
    }
}
