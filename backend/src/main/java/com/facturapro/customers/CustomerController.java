package com.facturapro.customers;

import com.facturapro.common.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMERS_VIEW')")
    public PageResponse<CustomerDtos.Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerService.list(search, active, Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMERS_VIEW')")
    public CustomerDtos.Response get(@PathVariable Long id) {
        return customerService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMERS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDtos.Response create(@Valid @RequestBody CustomerDtos.Request request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMERS_WRITE')")
    public CustomerDtos.Response update(@PathVariable Long id, @Valid @RequestBody CustomerDtos.Request request) {
        return customerService.update(id, request);
    }

    @PostMapping("/quick")
    @PreAuthorize("hasAuthority('CUSTOMERS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDtos.Response quickCreate(@Valid @RequestBody CustomerDtos.QuickCreateRequest request) {
        return CustomerDtos.Response.from(customerService.quickCreate(request));
    }
}
