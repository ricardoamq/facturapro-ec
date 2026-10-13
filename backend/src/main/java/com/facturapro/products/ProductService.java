package com.facturapro.products;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.ConflictException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.common.web.PageResponse;
import com.facturapro.inventory.StockService;
import com.facturapro.settings.SettingsService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockService stockService;
    private final SettingsService settingsService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProductDtos.Response> list(String search, Long categoryId, Boolean active,
                                                   Boolean lowStock, int page, int size, String sort) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("barcode")), like)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (Boolean.TRUE.equals(lowStock)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("stock"), root.get("minStock")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Sort sortOrder = switch (sort == null ? "" : sort) {
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            case "-name" -> Sort.by(Sort.Direction.DESC, "name");
            case "price" -> Sort.by(Sort.Direction.ASC, "salePrice");
            case "-price" -> Sort.by(Sort.Direction.DESC, "salePrice");
            case "stock" -> Sort.by(Sort.Direction.ASC, "stock");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
        Page<Product> result = productRepository.findAll(spec, PageRequest.of(page, size, sortOrder));
        return PageResponse.from(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductDtos.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado."));
    }

    @Transactional
    public ProductDtos.Response create(ProductDtos.CreateRequest request) {
        validateSku(request.sku(), null);
        validateBarcode(request.barcode(), null);
        Product product = new Product();
        product.setSku(request.sku().trim().toUpperCase());
        product.setName(request.name().trim());
        product.setDescription(blankToNull(request.description()));
        product.setCategory(findCategory(request.categoryId()));
        product.setPurchasePrice(request.purchasePrice());
        product.setSalePrice(request.salePrice());
        product.setUnit(request.unit().trim());
        product.setBarcode(blankToNull(request.barcode()));
        product.setMinStock(request.minStock());
        product.setTaxCategory(request.taxCategory() == null ? TaxCategory.GENERAL : request.taxCategory());
        product.setActive(request.active() == null || request.active());
        product.setStock(BigDecimal.ZERO);
        Product saved = productRepository.save(product);

        if (request.initialStock() != null && request.initialStock().signum() > 0) {
            stockService.apply(saved.getId(), com.facturapro.inventory.MovementType.ADJUSTMENT,
                    request.initialStock(), request.purchasePrice(), "PRODUCT", saved.getId(),
                    "Existencia inicial del producto");
            productRepository.flush();
        }
        auditService.record("PRODUCT_CREATED", "PRODUCT", saved.getId(),
                "Producto " + saved.getSku() + " - " + saved.getName());
        return toResponse(find(saved.getId()));
    }

    @Transactional
    public ProductDtos.Response update(Long id, ProductDtos.UpdateRequest request) {
        Product product = find(id);
        validateSku(request.sku(), id);
        validateBarcode(request.barcode(), id);
        product.setSku(request.sku().trim().toUpperCase());
        product.setName(request.name().trim());
        product.setDescription(blankToNull(request.description()));
        product.setCategory(findCategory(request.categoryId()));
        product.setPurchasePrice(request.purchasePrice());
        product.setSalePrice(request.salePrice());
        product.setUnit(request.unit().trim());
        product.setBarcode(blankToNull(request.barcode()));
        product.setMinStock(request.minStock());
        product.setTaxCategory(request.taxCategory() == null ? TaxCategory.GENERAL : request.taxCategory());
        if (request.active() != null) {
            product.setActive(request.active());
        }
        Product saved = productRepository.save(product);
        auditService.record("PRODUCT_UPDATED", "PRODUCT", saved.getId(),
                "Producto " + saved.getSku() + " actualizado");
        return toResponse(saved);
    }

    /**
     * Los productos con movimientos o ventas nunca se eliminan fisicamente:
     * solo se desactivan, para preservar la integridad de los documentos historicos.
     */
    @Transactional
    public void deactivate(Long id) {
        Product product = find(id);
        product.setActive(false);
        productRepository.save(product);
        auditService.record("PRODUCT_DEACTIVATED", "PRODUCT", id, "Producto " + product.getSku() + " desactivado");
    }

    @Transactional
    public void delete(Long id) {
        Product product = find(id);
        if (stockService.hasMovements(id)) {
            throw new ConflictException(
                    "El producto tiene movimientos de inventario o ventas asociadas. Use la desactivacion en su lugar.");
        }
        productRepository.delete(product);
        auditService.record("PRODUCT_DELETED", "PRODUCT", id, "Producto " + product.getSku() + " eliminado");
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.CategoryResponse> categories(Boolean active) {
        Specification<Category> spec = (root, query, cb) ->
                active == null ? cb.conjunction() : cb.equal(root.get("active"), active);
        return categoryRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(ProductDtos.CategoryResponse::from)
                .toList();
    }

    @Transactional
    public ProductDtos.CategoryResponse createCategory(ProductDtos.CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Ya existe una categoria con ese nombre.");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setDescription(blankToNull(request.description()));
        category.setActive(request.active() == null || request.active());
        return ProductDtos.CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public ProductDtos.CategoryResponse updateCategory(Long id, ProductDtos.CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria no encontrada."));
        categoryRepository.findByNameIgnoreCase(request.name().trim())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new ConflictException("Ya existe una categoria con ese nombre.");
                });
        category.setName(request.name().trim());
        category.setDescription(blankToNull(request.description()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return ProductDtos.CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public BigDecimal taxRateFor(Product product) {
        return settingsService.getTaxRate(product.getTaxCategory().name());
    }

    private ProductDtos.Response toResponse(Product product) {
        return ProductDtos.Response.from(product, taxRateFor(product));
    }

    private void validateSku(String sku, Long currentId) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        String normalized = sku.trim().toUpperCase();
        boolean exists = currentId == null
                ? productRepository.existsBySku(normalized)
                : productRepository.existsBySkuAndIdNot(normalized, currentId);
        if (exists) {
            throw new ConflictException("Ya existe un producto con el SKU " + normalized + ".");
        }
    }

    private void validateBarcode(String barcode, Long currentId) {
        String normalized = blankToNull(barcode);
        if (normalized == null) {
            return;
        }
        boolean exists = currentId == null
                ? productRepository.existsByBarcode(normalized)
                : productRepository.existsByBarcodeAndIdNot(normalized, currentId);
        if (exists) {
            throw new ConflictException("Ya existe un producto con ese codigo de barras.");
        }
    }

    private Category findCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "La categoria indicada no existe."));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
