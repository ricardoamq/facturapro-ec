package com.facturapro.users;

import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.ConflictException;
import com.facturapro.common.error.NotFoundException;
import com.facturapro.common.web.PageResponse;
import com.facturapro.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public PageResponse<UserDtos.Response> list(String search, Boolean active, int page, int size) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> result = userRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(result, UserDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public UserDtos.Response get(Long id) {
        return UserDtos.Response.from(find(id));
    }

    @Transactional
    public UserDtos.Response create(UserDtos.CreateRequest request) {
        if (userRepository.existsByUsername(request.username().trim())) {
            throw new ConflictException("Ya existe un usuario con ese nombre de usuario.");
        }
        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmail(request.email().trim())) {
            throw new ConflictException("Ya existe un usuario con ese correo electronico.");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setFullName(request.fullName().trim());
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setMustChangePassword(true);
        user.setActive(true);
        User saved = userRepository.save(user);
        auditService.record("USER_CREATED", "USER", saved.getId(),
                "Usuario " + saved.getUsername() + " con rol " + saved.getRole());
        return UserDtos.Response.from(saved);
    }

    @Transactional
    public UserDtos.Response update(Long id, UserDtos.UpdateRequest request) {
        User user = find(id);
        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmail(request.email().trim())
                && !request.email().trim().equalsIgnoreCase(user.getEmail())) {
            throw new ConflictException("Ya existe un usuario con ese correo electronico.");
        }
        boolean disablingSelf = user.isActive() && Boolean.FALSE.equals(request.active())
                && user.getId().equals(currentUser.id());
        if (disablingSelf) {
            throw new BusinessException("VALIDATION_ERROR", "No puede desactivar su propia cuenta.");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setRole(request.role());
        if (request.active() != null) {
            user.setActive(request.active());
        }
        User saved = userRepository.save(user);
        auditService.record("USER_UPDATED", "USER", saved.getId(),
                "Usuario " + saved.getUsername() + " actualizado");
        return UserDtos.Response.from(saved);
    }

    @Transactional
    public void adminResetPassword(Long id, UserDtos.AdminResetPasswordRequest request) {
        User user = find(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(request.mustChangePassword() == null || request.mustChangePassword());
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);
        auditService.record("USER_PASSWORD_RESET", "USER", user.getId(),
                "Contrasena restablecida por administrador para " + user.getUsername());
    }

    @Transactional
    public void changeOwnPassword(UserDtos.ChangePasswordRequest request) {
        User user = find(currentUser.id());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "La contrasena actual no es correcta.");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BusinessException("VALIDATION_ERROR", "La nueva contrasena debe ser diferente a la actual.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        auditService.record("PASSWORD_CHANGED", "USER", user.getId(), "Cambio de contrasena propio");
    }

    @Transactional(readOnly = true)
    public User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));
    }

    @Transactional
    public void recordSuccessfulLogin(User user) {
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void recordFailedLogin(User user, int maxAttempts, int lockMinutes) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);
        if (user.getFailedAttempts() >= maxAttempts) {
            user.setLockedUntil(Instant.now().plusSeconds(lockMinutes * 60L));
        }
        userRepository.save(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
