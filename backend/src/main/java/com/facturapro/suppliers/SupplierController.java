package com.facturapro.suppliers;

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
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIERS_VIEW')")
    public PageResponse<SupplierDtos.Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return supplierService.list(search, active, Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIERS_VIEW')")
    public SupplierDtos.Response get(@PathVariable Long id) {
        return supplierService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIERS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDtos.Response create(@Valid @RequestBody SupplierDtos.Request request) {
        return supplierService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIERS_WRITE')")
    public SupplierDtos.Response update(@PathVariable Long id, @Valid @RequestBody SupplierDtos.Request request) {
        return supplierService.update(id, request);
    }
}
