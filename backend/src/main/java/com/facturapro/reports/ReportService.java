package com.facturapro.reports;

import com.facturapro.common.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes con datos reales. Distingue explicitamente:
 *  - ingresos por ventas (total de documentos emitidos)
 *  - cobros recibidos (pagos no anulados)
 *  - costos (costo de mercancia vendida, calculado con el costo congelado en cada linea)
 *  - utilidad estimada (ingresos netos de impuestos - costos)
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final ReportRepository reportRepository;
    private final com.facturapro.products.ProductRepository productRepository;
    private final com.facturapro.settings.SettingsService settingsService;

    public record DayRow(LocalDate date, long salesCount, BigDecimal subtotal, BigDecimal discount,
                         BigDecimal tax, BigDecimal total) {
    }

    public record ProductRow(String sku, String name, BigDecimal quantity, BigDecimal netSales,
                             BigDecimal tax, BigDecimal total, BigDecimal cost, BigDecimal profit) {
    }

    public record PartyRow(String identification, String name, long salesCount, BigDecimal total,
                           BigDecimal netSales, BigDecimal cost, BigDecimal profit) {
    }

    public record TaxRow(BigDecimal rate, BigDecimal taxAmount, BigDecimal netSales) {
    }

    public record SalesSummary(LocalDate from, LocalDate to, long salesCount, BigDecimal totalSales,
                               BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxTotal,
                               BigDecimal paymentsReceived, BigDecimal estimatedCost, BigDecimal estimatedProfit,
                               List<DayRow> byDay) {
    }

    public record ReceivableRow(Long saleId, String number, String customerIdentification, String customerName,
                                LocalDate saleDate, BigDecimal total, BigDecimal amountPaid, BigDecimal balance,
                                long daysOpen) {
    }

    public record InventoryRow(Long productId, String sku, String name, String categoryName,
                               BigDecimal stock, BigDecimal minStock, BigDecimal unitCost,
                               BigDecimal stockValue, boolean lowStock) {
    }

    public record PurchaseDayRow(LocalDate date, long purchasesCount, BigDecimal subtotal, BigDecimal tax,
                                 BigDecimal total) {
    }

    public record PurchasesSummary(LocalDate from, LocalDate to, long purchasesCount, BigDecimal total,
                                   BigDecimal subtotal, BigDecimal tax, List<PurchaseDayRow> byDay) {
    }

    @Transactional(readOnly = true)
    public SalesSummary salesSummary(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<Object[]> totalsRows = reportRepository.salesTotals(from, to);
        Object[] totals = totalsRows.isEmpty() ? null : totalsRows.get(0);
        long count = totals == null || totals[1] == null ? 0 : ((Number) totals[1]).longValue();
        BigDecimal totalSales = totals == null || totals[0] == null ? BigDecimal.ZERO : (BigDecimal) totals[0];

        List<DayRow> byDay = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (Object[] row : reportRepository.salesByDay(from, to)) {
            DayRow day = new DayRow((LocalDate) row[0], ((Number) row[1]).longValue(),
                    (BigDecimal) row[2], (BigDecimal) row[3], (BigDecimal) row[4], (BigDecimal) row[5]);
            byDay.add(day);
            subtotal = subtotal.add(day.subtotal());
            discount = discount.add(day.discount());
            tax = tax.add(day.tax());
        }
        BigDecimal payments = paymentsReceived(from, to);
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal netSales = BigDecimal.ZERO;
        for (ProductRow row : salesByProduct(from, to)) {
            cost = cost.add(row.cost());
            netSales = netSales.add(row.netSales());
        }
        return new SalesSummary(from, to, count, totalSales, subtotal, discount, tax,
                payments, cost, netSales.subtract(cost), byDay);
    }

    @Transactional(readOnly = true)
    public List<ProductRow> salesByProduct(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<ProductRow> rows = new ArrayList<>();
        for (Object[] row : reportRepository.salesByProduct(from, to)) {
            BigDecimal net = (BigDecimal) row[3];
            BigDecimal cost = (BigDecimal) row[6] == null ? BigDecimal.ZERO : (BigDecimal) row[6];
            rows.add(new ProductRow((String) row[0], (String) row[1], (BigDecimal) row[2],
                    net, (BigDecimal) row[4], (BigDecimal) row[5], cost, net.subtract(cost)));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<PartyRow> salesByCustomer(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<PartyRow> rows = new ArrayList<>();
        for (Object[] row : reportRepository.salesByCustomer(from, to)) {
            BigDecimal net = (BigDecimal) row[4];
            BigDecimal cost = row[5] == null ? BigDecimal.ZERO : (BigDecimal) row[5];
            rows.add(new PartyRow((String) row[0], (String) row[1], ((Number) row[2]).longValue(),
                    (BigDecimal) row[3], net, cost, net.subtract(cost)));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<PartyRow> salesByUser(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<PartyRow> rows = new ArrayList<>();
        for (Object[] row : reportRepository.salesByUser(from, to)) {
            BigDecimal net = (BigDecimal) row[4];
            BigDecimal cost = row[5] == null ? BigDecimal.ZERO : (BigDecimal) row[5];
            rows.add(new PartyRow(String.valueOf(row[0]), (String) row[1], ((Number) row[2]).longValue(),
                    (BigDecimal) row[3], net, cost, net.subtract(cost)));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<TaxRow> taxes(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<TaxRow> rows = new ArrayList<>();
        for (Object[] row : reportRepository.taxesByRate(from, to)) {
            rows.add(new TaxRow((BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2]));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<ReceivableRow> receivables() {
        LocalDate today = LocalDate.now(ECUADOR);
        return reportRepository.receivables().stream()
                .map(s -> new ReceivableRow(s.getId(), s.getNumber(), s.getCustomerIdentification(),
                        s.getCustomerName(), s.getSaleDate(), s.getTotal(), s.getAmountPaid(), s.getBalance(),
                        java.time.temporal.ChronoUnit.DAYS.between(s.getSaleDate(), today)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryRow> inventory() {
        return productRepository.findAll().stream()
                .filter(com.facturapro.products.Product::isActive)
                .map(p -> new InventoryRow(
                        p.getId(), p.getSku(), p.getName(),
                        p.getCategory() == null ? null : p.getCategory().getName(),
                        p.getStock(), p.getMinStock(), p.getPurchasePrice(),
                        p.getStock().multiply(p.getPurchasePrice()),
                        p.getStock().compareTo(p.getMinStock()) <= 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchasesSummary purchases(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<PurchaseDayRow> byDay = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        for (Object[] row : reportRepository.purchasesByDay(from, to)) {
            PurchaseDayRow day = new PurchaseDayRow((LocalDate) row[0], ((Number) row[1]).longValue(),
                    (BigDecimal) row[2], (BigDecimal) row[3], (BigDecimal) row[4]);
            byDay.add(day);
            count += day.purchasesCount();
            subtotal = subtotal.add(day.subtotal());
            tax = tax.add(day.tax());
            total = total.add(day.total());
        }
        return new PurchasesSummary(from, to, count, total, subtotal, tax, byDay);
    }

    @Transactional(readOnly = true)
    public List<String> exportSalesCsv(LocalDate from, LocalDate to) {
        List<String> lines = new ArrayList<>();
        lines.add("Fecha;Numero;Cliente;Identificacion;Subtotal;Descuento;IVA;Total;Pagado;Saldo;Estado");
        for (var sale : reportRepository.issuedSalesBetween(from, to)) {
            lines.add(String.join(";",
                    sale.getSaleDate().toString(),
                    nullToEmpty(sale.getNumber()),
                    nullToEmpty(sale.getCustomerName()),
                    nullToEmpty(sale.getCustomerIdentification()),
                    sale.getSubtotal().toPlainString(),
                    sale.getDiscountTotal().toPlainString(),
                    sale.getTaxTotal().toPlainString(),
                    sale.getTotal().toPlainString(),
                    sale.getAmountPaid().toPlainString(),
                    sale.getBalance().toPlainString(),
                    sale.getStatus().name()));
        }
        return lines;
    }

    private BigDecimal paymentsReceived(LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ECUADOR).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ECUADOR).toInstant();
        List<Object[]> rows = reportRepository.paymentsTotals(fromInstant, toInstant);
        Object[] result = rows.isEmpty() ? null : rows.get(0);
        if (result == null || result[0] == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return (BigDecimal) result[0];
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("VALIDATION_ERROR", "Debe indicar las fechas desde y hasta.");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("VALIDATION_ERROR", "La fecha inicial no puede ser posterior a la final.");
        }
        if (from.plusYears(3).isBefore(to)) {
            throw new BusinessException("VALIDATION_ERROR", "El rango maximo consultable es de 3 anios.");
        }
    }

    public LocalDate firstDayOfCurrentMonth() {
        return LocalDate.now(ECUADOR).with(TemporalAdjusters.firstDayOfMonth());
    }

    public LocalDate today() {
        return LocalDate.now(ECUADOR);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
