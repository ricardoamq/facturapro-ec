package com.facturapro;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de humo end-to-end con MockMvc: sesion, CSRF y los endpoints
 * principales del panel, reportes, inventario y comprobantes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpSession session;
    private static String csrfToken;
    private static String saleNumber;

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    private MvcResult csrfGet() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        assertNotNull(res.getResponse().getCookie("XSRF-TOKEN"), "Debe emitir la cookie XSRF-TOKEN");
        csrfToken = res.getResponse().getCookie("XSRF-TOKEN").getValue();
        return res;
    }

    @Test
    @Order(1)
    void loginCreaSesionValida() throws Exception {
        csrfGet();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .session(new MockHttpSession())
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrfToken))
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"AdminTest12345!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session, "El login debe crear una sesion (cookie JSESSIONID)");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @Order(2)
    void loginSinCsrfEsRechazado() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"AdminTest12345!\"}"))
                .andReturn();
        assertTrue(res.getResponse().getStatus() == 403 || res.getResponse().getStatus() == 400,
                "POST sin token CSRF debe rechazarse, obtuvo " + res.getResponse().getStatus());
    }

    @Test
    @Order(3)
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    void dashboardResponde200() throws Exception {
        assertNotNull(session, "La sesion del paso 1 es obligatoria");
        MvcResult res = mockMvc.perform(get("/api/dashboard/summary").session(session)).andReturn();
        if (res.getResponse().getStatus() != 200) {
            System.out.println("[DASHBOARD FALLO " + res.getResponse().getStatus() + "] " + body(res));
        }
        assertEquals(200, res.getResponse().getStatus(), "GET /api/dashboard/summary");
    }

    @Test
    @Order(5)
    void reporteDeVentasResponde200() throws Exception {
        assertNotNull(session);
        String from = LocalDate.now().minusDays(30).toString();
        String to = LocalDate.now().toString();
        MvcResult res = mockMvc.perform(
                        get("/api/reports/sales").session(session).param("from", from).param("to", to))
                .andReturn();
        if (res.getResponse().getStatus() != 200) {
            System.out.println("[REPORTES FALLO " + res.getResponse().getStatus() + "] " + body(res));
        }
        assertEquals(200, res.getResponse().getStatus(), "GET /api/reports/sales");
    }

    @Test
    @Order(6)
    void movimientosDeInventarioResponde200() throws Exception {
        assertNotNull(session);
        MvcResult res = mockMvc.perform(get("/api/inventory/movements").session(session).param("size", "5"))
                .andReturn();
        if (res.getResponse().getStatus() != 200) {
            System.out.println("[MOVIMIENTOS FALLO " + res.getResponse().getStatus() + "] " + body(res));
        }
        assertEquals(200, res.getResponse().getStatus(), "GET /api/inventory/movements");
    }

    @Test
    @Order(7)
    void ventaEmitidaYComprobantePdf() throws Exception {
        assertNotNull(session);

        MvcResult productRes = mockMvc.perform(post("/api/products")
                        .session(session)
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrfToken))
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"IT-%d","name":"Producto de integracion","purchasePrice":10,
                                 "salePrice":15,"unit":"UND","initialStock":50,"minStock":2,
                                 "taxCategory":"GENERAL","active":true}
                                """.formatted(System.nanoTime() % 1_000_000_000L)))
                .andExpect(status().isCreated())
                .andReturn();
        long productId = ((Number) com.jayway.jsonpath.JsonPath.read(body(productRes), "$.id")).longValue();

        MvcResult saleRes = mockMvc.perform(post("/api/sales")
                        .session(session)
                        .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrfToken))
                        .header("X-XSRF-TOKEN", csrfToken)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"saleDate":"%s","issue":true,
                                 "items":[{"productId":%d,"quantity":2,"unitPrice":15}]}
                                """.formatted(LocalDate.now(), productId)))
                .andReturn();
        if (saleRes.getResponse().getStatus() != 201) {
            System.out.println("[VENTA FALLO " + saleRes.getResponse().getStatus() + "] " + body(saleRes));
        }
        assertEquals(201, saleRes.getResponse().getStatus(), "POST /api/sales");
        saleNumber = com.jayway.jsonpath.JsonPath.read(body(saleRes), "$.number");
        assertNotNull(saleNumber, "La venta emitida debe tener numero");

        MvcResult pdf = mockMvc.perform(get("/api/invoices/" + saleNumber + "/pdf").session(session)).andReturn();
        if (pdf.getResponse().getStatus() != 200) {
            System.out.println("[PDF FALLO " + pdf.getResponse().getStatus() + "] " + body(pdf));
        }
        assertEquals(200, pdf.getResponse().getStatus(), "GET /api/invoices/{number}/pdf");
        assertTrue(pdf.getResponse().getContentAsByteArray().length > 500, "El PDF debe tener contenido");
    }

    @Test
    @Order(8)
    void endpointsDeLecturaBasicos() throws Exception {
        assertNotNull(session);
        mockMvc.perform(get("/api/products").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/sales").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/inventory/summary").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/settings").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/audit").session(session)).andExpect(status().isOk());
    }
}
