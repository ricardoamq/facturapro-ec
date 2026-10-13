package com.facturapro.audit;

import com.facturapro.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registro de auditoria de acciones criticas.
 * Nunca almacena contrasenas, tokens ni datos de pago sensibles: solo metadatos de la operacion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final CurrentUser currentUser;

    public void record(String action, String entityType, Object entityId, String details) {
        AuditLog entry = new AuditLog();
        try {
            entry.setUserId(currentUser.id());
            entry.setUsername(currentUser.username());
        } catch (Exception ignored) {
            // Accion sin usuario autenticado (p. ej. login fallido).
        }
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId == null ? null : String.valueOf(entityId));
        entry.setDetails(truncate(details));
        entry.setIp(currentIp());
        repository.save(entry);
        log.info("AUDIT {} {} {} - {}", entry.getUsername(), action, entityType, entry.getEntityId());
    }

    public void recordAs(String username, Long userId, String action, String entityType, Object entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId == null ? null : String.valueOf(entityId));
        entry.setDetails(truncate(details));
        entry.setIp(currentIp());
        repository.save(entry);
        log.info("AUDIT {} {} {} - {}", username, action, entityType, entry.getEntityId());
    }

    private String truncate(String details) {
        if (details == null) {
            return null;
        }
        return details.length() > 1000 ? details.substring(0, 1000) : details;
    }

    private String currentIp() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest request = servletAttrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // Sin contexto de peticion.
        }
        return null;
    }
}
