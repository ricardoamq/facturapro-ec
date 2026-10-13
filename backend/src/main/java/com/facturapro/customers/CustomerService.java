package com.facturapro.customers;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.ConflictException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.common.util.IdentificationValidator;
import com.facturapro.common.web.PageResponse;
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
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerDtos.Response> list(String search, Boolean active, int page, int size) {
        Specification<Customer> spec = (root, query, cb) -> {
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
        Page<Customer> result = customerRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "businessName")));
        return PageResponse.from(result, CustomerDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public CustomerDtos.Response get(Long id) {
        return CustomerDtos.Response.from(find(id));
    }

    @Transactional(readOnly = true)
    public Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado."));
    }

    @Transactional
    public CustomerDtos.Response create(CustomerDtos.Request request) {
        validateIdentification(request);
        String identification = request.identification().trim().toUpperCase();
        if (customerRepository.existsByIdentification(identification)) {
            throw new ConflictException("Ya existe un cliente con esa identificacion.");
        }
        Customer customer = new Customer();
        apply(customer, request);
        customer.setIdentification(identification);
        Customer saved = customerRepository.save(customer);
        auditService.record("CUSTOMER_CREATED", "CUSTOMER", saved.getId(),
                "Cliente " + saved.getIdentification() + " - " + saved.getBusinessName());
        return CustomerDtos.Response.from(saved);
    }

    @Transactional
    public CustomerDtos.Response update(Long id, CustomerDtos.Request request) {
        Customer customer = find(id);
        validateIdentification(request);
        String identification = request.identification().trim().toUpperCase();
        customerRepository.existsByIdentificationAndIdNot(identification, id);
        if (!identification.equals(customer.getIdentification())
                && customerRepository.existsByIdentification(identification)) {
            throw new ConflictException("Ya existe un cliente con esa identificacion.");
        }
        customer.setIdentification(identification);
        apply(customer, request);
        Customer saved = customerRepository.save(customer);
        auditService.record("CUSTOMER_UPDATED", "CUSTOMER", saved.getId(), "Cliente " + saved.getIdentification());
        return CustomerDtos.Response.from(saved);
    }

    /** Alta rapida desde la pantalla de ventas: valida formato pero no impide duplicados por identificacion. */
    @Transactional
    public Customer quickCreate(CustomerDtos.QuickCreateRequest request) {
        IdType type = inferIdType(request.identification());
        if (!IdentificationValidator.isValid(request.identification().trim(), type.name())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "La identificacion no cumple el formato esperado para " + type.name() + ".");
        }
        Customer existing = customerRepository.findByIdentification(request.identification().trim().toUpperCase())
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        Customer customer = new Customer();
        customer.setIdType(type);
        customer.setIdentification(request.identification().trim().toUpperCase());
        customer.setBusinessName(request.businessName().trim());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        Customer saved = customerRepository.save(customer);
        auditService.record("CUSTOMER_CREATED", "CUSTOMER", saved.getId(),
                "Cliente rapido " + saved.getIdentification());
        return saved;
    }

    private IdType inferIdType(String identification) {
        String value = identification == null ? "" : identification.trim();
        if (value.matches("\\d{13}")) {
            return IdType.RUC;
        }
        if (value.matches("\\d{10}")) {
            return IdType.CEDULA;
        }
        return IdType.PASAPORTE;
    }

    private void validateIdentification(CustomerDtos.Request request) {
        String value = request.identification() == null ? "" : request.identification().trim();
        if (!IdentificationValidator.isValid(value, request.idType().name())) {
            throw new BusinessException("VALIDATION_ERROR",
                    "La identificacion no es valida para el tipo " + request.idType().label()
                            + ". Verifique el formato (cedula: 10 digitos; RUC: 13 digitos).");
        }
    }

    private void apply(Customer customer, CustomerDtos.Request request) {
        customer.setIdType(request.idType());
        customer.setBusinessName(request.businessName().trim());
        customer.setAddress(blankToNull(request.address()));
        customer.setPhone(blankToNull(request.phone()));
        customer.setEmail(blankToNull(request.email()));
        customer.setNotes(blankToNull(request.notes()));
        if (request.active() != null) {
            customer.setActive(request.active());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
