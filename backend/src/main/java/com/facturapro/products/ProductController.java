package com.facturapro.products;

import com.facturapro.common.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public PageResponse<ProductDtos.Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String sort) {
        return productService.list(search, categoryId, active, lowStock,
                Math.max(0, page), Math.min(100, Math.max(1, size)), sort);
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public ProductDtos.Response get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('PRODUCTS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDtos.Response create(@Valid @RequestBody ProductDtos.CreateRequest request) {
        return productService.create(request);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PRODUCTS_WRITE')")
    public ProductDtos.Response update(@PathVariable Long id, @Valid @RequestBody ProductDtos.UpdateRequest request) {
        return productService.update(id, request);
    }

    @PostMapping("/products/{id}/deactivate")
    @PreAuthorize("hasAuthority('PRODUCTS_WRITE')")
    public ProductDtos.Response deactivate(@PathVariable Long id) {
        productService.deactivate(id);
        return productService.get(id);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority('PRODUCTS_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('PRODUCTS_VIEW')")
    public List<ProductDtos.CategoryResponse> categories(@RequestParam(required = false) Boolean active) {
        return productService.categories(active);
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('PRODUCTS_WRITE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDtos.CategoryResponse createCategory(@Valid @RequestBody ProductDtos.CategoryRequest request) {
        return productService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('PRODUCTS_WRITE')")
    public ProductDtos.CategoryResponse updateCategory(@PathVariable Long id,
                                                       @Valid @RequestBody ProductDtos.CategoryRequest request) {
        return productService.updateCategory(id, request);
    }
}
