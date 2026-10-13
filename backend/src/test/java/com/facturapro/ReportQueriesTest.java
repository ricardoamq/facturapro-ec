package com.facturapro;

import com.facturapro.reports.ReportQueries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regresion: las consultas de fila unica deben devolver filas planas de 2 columnas
 * (anteriormente el retorno Object[] producia un array anidado y provocaba 500).
 */
@SpringBootTest
@ActiveProfiles("test")
class ReportQueriesTest {

    @Autowired
    private ReportQueries reportQueries;

    @Test
    void salesTotalsDevuelveFilaPlana() {
        List<Object[]> rows = reportQueries.salesTotals(LocalDate.now().minusDays(30), LocalDate.now());
        assertFalse(rows.isEmpty(), "La agregacion sin GROUP BY siempre devuelve una fila");
        assertEquals(2, rows.get(0).length, "Debe tener [suma, contador]");
    }

    @Test
    void paymentsTotalsDevuelveFilaPlana() {
        ZoneId ecuador = ZoneId.of("America/Guayaquil");
        List<Object[]> rows = reportQueries.paymentsTotals(
                LocalDate.now().minusDays(30).atStartOfDay(ecuador).toInstant(),
                LocalDate.now().plusDays(1).atStartOfDay(ecuador).toInstant());
        assertFalse(rows.isEmpty(), "La agregacion sin GROUP BY siempre devuelve una fila");
        assertEquals(2, rows.get(0).length, "Debe tener [suma, contador]");
    }
}
