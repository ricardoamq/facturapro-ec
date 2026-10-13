package com.facturapro.config;

import com.facturapro.audit.AuditService;
import com.facturapro.users.Role;
import com.facturapro.users.User;
import com.facturapro.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Crea el usuario administrador inicial si la base de datos esta vacia.
 * La contrasena proviene de APP_ADMIN_INITIAL_PASSWORD; si no se define,
 * se genera una aleatoria que se imprime UNA sola vez en la consola del backend
 * y el usuario debe cambiarla en el primer inicio de sesion.
 * Nunca se usa una contrasena universal ni se versiona ningun secreto.
 */
@Slf4j
@Component
@org.springframework.core.annotation.Order(1)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsAny()) {
            return;
        }
        String username = appProperties.admin().initialUsername();
        String configured = appProperties.admin().initialPassword();
        String password = configured != null && !configured.isBlank() ? configured : generatePassword();

        User admin = new User();
        admin.setUsername(username);
        admin.setFullName(appProperties.admin().initialFullName());
        admin.setEmail(blankToNull(appProperties.admin().initialEmail()));
        admin.setRole(Role.ADMINISTRADOR);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setActive(true);
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        auditService.recordAs(username, admin.getId(), "USER_CREATED", "USER", admin.getId(),
                "Usuario administrador inicial");

        if (configured == null || configured.isBlank()) {
            log.warn("=".repeat(72));
            log.warn(" USUARIO ADMINISTRADOR INICIAL CREADO");
            log.warn("   Usuario : {}", username);
            log.warn("   Contrasena temporal (guarde ahora, solo se muestra una vez): {}", password);
            log.warn("   Debera cambiarla en el primer inicio de sesion.");
            log.warn(" Para definirla usted mismo use la variable APP_ADMIN_INITIAL_PASSWORD.");
            log.warn("=".repeat(72));
        } else {
            log.info("Usuario administrador inicial '{}' creado con la contrasena definida en el entorno.", username);
        }
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
