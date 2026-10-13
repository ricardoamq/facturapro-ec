package com.facturapro.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('PAYMENTS_VOID')")
    public PaymentDtos.Response voidPayment(@PathVariable Long id,
                                            @jakarta.validation.Valid @RequestBody PaymentDtos.VoidRequest request) {
        return PaymentDtos.Response.from(paymentService.voidPayment(id, request.reason()));
    }
}
