package com.facturapro.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * BCrypt con factor 12 (>= 10 recomendado por OWASP para BCrypt).
     * Se expone como constante para que servicios internos puedan reutilizarlo.
     */
    public static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return ENCODER;
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/csrf",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/actuator/**").hasAuthority("SETTINGS_VIEW")
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, 401, "UNAUTHORIZED", "Debe iniciar sesion para acceder a este recurso.", request.getRequestURI()))
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(new DevCorsLogFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeError(response, 403, "FORBIDDEN", "No tiene permisos para realizar esta operacion.", request.getRequestURI());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(appProperties.cors().allowedOrigins().split(","))
                .map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String code,
                            String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                com.facturapro.common.web.ApiError.of(status, status == 401 ? "Unauthorized" : "Forbidden", code, message, path));
    }

    /**
     * Manejador CSRF para SPA.
     * Contrato documentado: el frontend lee la cookie XSRF-TOKEN (no HttpOnly)
     * y la envia sin modificar en la cabecera X-XSRF-TOKEN.
     * Se usa el manejador por defecto (token sin enmascarar) para que el valor
     * del JSON de /api/auth/csrf y el de la cookie coincidan siempre.
     */
    static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return headerValue != null ? headerValue : super.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    /** Filtro inocuo reservado para futuras verificaciones de origen en desarrollo. */
    static class DevCorsLogFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            filterChain.doFilter(request, response);
        }
    }
}
