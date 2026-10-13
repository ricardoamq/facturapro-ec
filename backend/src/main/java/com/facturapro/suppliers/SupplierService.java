package com.facturapro.suppliers;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.ConflictException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.common.util.IdentificationValidator;
import com.facturapro.common.web.PageResponse;
import com.facturapro.customers.IdType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<SupplierDtos.Response> list(String search, Boolean active, int page, int size) {
        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("identification")), like),
                        cb.like(cb.lower(root.get("businessName")), like)));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Supplier> result = supplierRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "businessName")));
        return PageResponse.from(result, SupplierDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public Supplier find(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Proveedor no encontrado."));
    }

    @Transactional(readOnly = true)
    public SupplierDtos.Response get(Long id) {
        return SupplierDtos.Response.from(find(id));
    }

    @Transactional
    public SupplierDtos.Response create(SupplierDtos.Request request) {
        validateIdentification(request);
        String identification = request.identification().trim().toUpperCase();
        if (supplierRepository.existsByIdentification(identification)) {
            throw new ConflictException("Ya existe un proveedor con esa identificacion.");
        }
        Supplier supplier = new Supplier();
        apply(supplier, request);
        supplier.setIdentification(identification);
        Supplier saved = supplierRepository.save(supplier);
        auditService.record("SUPPLIER_CREATED", "SUPPLIER", saved.getId(),
                "Proveedor " + saved.getIdentification() + " - " + saved.getBusinessName());
        return SupplierDtos.Response.from(saved);
    }

    @Transactional
    public SupplierDtos.Response update(Long id, SupplierDtos.Request request) {
        Supplier supplier = find(id);
        validateIdentification(request);
        String identification = request.identification().trim().toUpperCase();
        if (!identification.equals(supplier.getIdentification())
                && supplierRepository.existsByIdentification(identification)) {
            throw new ConflictException("Ya existe un proveedor con esa identificacion.");
        }
        supplier.setIdentification(identification);
        apply(supplier, request);
        Supplier saved = supplierRepository.save(supplier);
        auditService.record("SUPPLIER_UPDATED", "SUPPLIER", saved.getId(), "Proveedor " + saved.getIdentification());
        return SupplierDtos.Response.from(saved);
    }

    private void validateIdentification(SupplierDtos.Request request) {
        String value = request.identification() == null ? "" : request.identification().trim();
        if (!IdentificationValidator.isValid(value, request.idType().name())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "La identificacion no es valida para el tipo " + request.idType().label() + ".");
        }
    }

    private void apply(Supplier supplier, SupplierDtos.Request request) {
        supplier.setIdType(request.idType());
        supplier.setBusinessName(request.businessName().trim());
        supplier.setAddress(blankToNull(request.address()));
        supplier.setPhone(blankToNull(request.phone()));
        supplier.setEmail(blankToNull(request.email()));
        supplier.setNotes(blankToNull(request.notes()));
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
