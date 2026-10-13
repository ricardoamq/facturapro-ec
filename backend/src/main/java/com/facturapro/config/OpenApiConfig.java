package com.facturapro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI facturaproOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FacturaPro EC API")
                        .description("""
                                API del sistema de facturacion e inventario FacturaPro EC.

                                Autenticacion: sesion basada en cookies (JSESSIONID, HttpOnly).
                                Antes de enviar mutaciones (POST/PUT/DELETE) obtenga el token CSRF
                                en GET /api/auth/csrf y envuelvalo en la cabecera X-XSRF-TOKEN.

                                Los documentos emitidos son comprobantes INTERNOS de gestion.
                                No constituyen comprobantes de venta autorizados por el SRI.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("FacturaPro EC")))
                .components(new Components().addSecuritySchemes("cookieAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
    }
}
