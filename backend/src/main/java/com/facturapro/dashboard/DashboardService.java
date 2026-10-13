package com.facturapro.dashboard;

import com.facturapro.audit.AuditLogRepository;
import com.facturapro.products.ProductRepository;
import com.facturapro.reports.ReportQueries;
import com.facturapro.sales.PaymentStatus;
import com.facturapro.sales.SaleRepository;
import com.facturapro.sales.SaleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Indicadores del panel principal calculados sobre datos reales.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final com.facturapro.reports.ReportRepository reportQueries;
    private final AuditLogRepository auditLogRepository;

    public record DaySales(LocalDate date, long count, BigDecimal total) {
    }

    public record TopProduct(String sku, String name, BigDecimal quantity, BigDecimal total) {
    }

    public record RecentActivity(Long id, String username, String action, String entityType,
                                 String entityId, String details, java.time.Instant createdAt) {
    }

    public record Summary(
            DaySales today,
            DaySales week,
            DaySales month,
            long lowStockCount,
            long pendingInvoicesCount,
            BigDecimal receivablesTotal,
            BigDecimal stockValue,
            List<TopProduct> topProducts,
            List<DaySales> evolution,
            List<RecentActivity> recentActivity) {
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        LocalDate today = LocalDate.now(ECUADOR);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate evolutionStart = today.minusDays(29);

        List<DaySales> byDay = new ArrayList<>();
        for (Object[] row : reportQueries.salesByDay(evolutionStart, today)) {
            byDay.add(new DaySales((LocalDate) row[0], ((Number) row[1]).longValue(), (BigDecimal) row[5]));
        }
        List<DaySales> completeEvolution = new ArrayList<>();
        for (LocalDate date = evolutionStart; !date.isAfter(today); date = date.plusDays(1)) {
            LocalDate d = date;
            completeEvolution.add(byDay.stream()
                    .filter(row -> row.date().equals(d))
                    .findFirst()
                    .orElseGet(() -> new DaySales(d, 0, BigDecimal.ZERO)));
        }

        List<TopProduct> topProducts = reportQueries.topProducts(today.minusDays(30), today,
                        PageRequest.of(0, 5)).stream()
                .map(row -> new TopProduct((String) row[0], (String) row[1],
                        (BigDecimal) row[3], (BigDecimal) row[4]))
                .toList();

        List<RecentActivity> recent = auditLogRepository.findAll(
                        PageRequest.of(0, 12, org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "id"))).stream()
                .map(a -> new RecentActivity(a.getId(), a.getUsername(), a.getAction(), a.getEntityType(),
                        a.getEntityId(), a.getDetails(), a.getCreatedAt()))
                .toList();

        long pendingInvoices = saleRepository.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.ISSUED)
                .filter(s -> s.getPaymentStatus() != PaymentStatus.PAID)
                .count();
        BigDecimal receivables = saleRepository.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.ISSUED)
                .map(s -> s.getBalance())
                .filter(b -> b != null && b.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Summary(
                salesBetween(today, today),
                salesBetween(weekStart, today),
                salesBetween(monthStart, today),
                productRepository.findLowStock().size(),
                pendingInvoices,
                receivables,
                productRepository.totalStockValue(),
                topProducts,
                completeEvolution,
                recent);
    }

    private DaySales salesBetween(LocalDate from, LocalDate to) {
        java.util.List<Object[]> rows = reportQueries.salesTotals(from, to);
        Object[] totals = rows.isEmpty() ? null : rows.get(0);
        long count = totals == null || totals[1] == null ? 0 : ((Number) totals[1]).longValue();
        BigDecimal total = totals == null || totals[0] == null ? BigDecimal.ZERO : (BigDecimal) totals[0];
        return new DaySales(from, count, total);
    }
}
