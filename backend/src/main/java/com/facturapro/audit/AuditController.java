package com.facturapro.audit;

import com.facturapro.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final AuditLogRepository repository;

    public record Response(Long id, String username, String action, String entityType, String entityId,
                           String details, String ip, Instant createdAt) {

        public static Response from(AuditLog log) {
            return new Response(log.getId(), log.getUsername(), log.getAction(), log.getEntityType(),
                    log.getEntityId(), log.getDetails(), log.getIp(), log.getCreatedAt());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public PageResponse<Response> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay(ECUADOR).toInstant()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay(ECUADOR).toInstant()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        var result = repository.findAll(spec,
                PageRequest.of(page, Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(result, Response::from);
    }
}
