package com.facturapro.invoices;

import com.facturapro.settings.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Asignacion transaccional de numeros de documento internos.
 * Usa bloqueo pesimista sobre la fila del contador, por lo que dos solicitudes
 * concurrentes no pueden obtener el mismo numero.
 * Formato: SERIE-ANIO-00000001 (ej. FAC-2026-00000001).
 */
@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    public static final String SALE = "SALE";
    public static final String PURCHASE = "PURCHASE";
    public static final String PURCHASE_RETURN = "PURCHASE_RETURN";

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final DocumentNumberRepository repository;
    private final SettingsService settingsService;

    @Transactional
    public String nextSaleNumber() {
        return next(SALE, settingsService.get("numbering.saleSeries"));
    }

    @Transactional
    public String nextPurchaseNumber() {
        return next(PURCHASE, settingsService.get("numbering.purchaseSeries"));
    }

    @Transactional
    public String nextPurchaseReturnNumber() {
        return next(PURCHASE_RETURN, "DEV");
    }

    @Transactional
    public String next(String docType, String series) {
        int year = LocalDate.now(ECUADOR).getYear();
        DocumentNumber counter = repository.findForUpdate(docType, series, year)
                .orElseGet(() -> {
                    DocumentNumber created = new DocumentNumber();
                    created.setDocType(docType);
                    created.setSeries(series);
                    created.setPeriodYear(year);
                    created.setLastNumber(0);
                    return created;
                });
        long next = counter.getLastNumber() + 1;
        counter.setLastNumber(next);
        repository.save(counter);
        return "%s-%d-%08d".formatted(series, year, next);
    }
}
