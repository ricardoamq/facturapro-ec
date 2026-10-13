package com.facturapro.reports;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de agregacion para reportes. Operan sobre datos reales de la base.
 */
public interface ReportQueries {

    @Query("""
            select s.saleDate, count(s), coalesce(sum(s.subtotal),0), coalesce(sum(s.discountTotal),0),
                   coalesce(sum(s.taxTotal),0), coalesce(sum(s.total),0)
            from Sale s
            where s.status = com.facturapro.sales.SaleStatus.ISSUED
              and s.saleDate between :from and :to
            group by s.saleDate
            order by s.saleDate
            """)
    List<Object[]> salesByDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select i.sku, i.productName, sum(i.quantity), sum(i.lineSubtotal), sum(i.lineTax), sum(i.lineTotal),
                   sum(i.quantity * i.unitCost)
            from SaleItem i
            where i.sale.status = com.facturapro.sales.SaleStatus.ISSUED
              and i.sale.saleDate between :from and :to
            group by i.sku, i.productName
            order by sum(i.lineTotal) desc
            """)
    List<Object[]> salesByProduct(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select i.sale.customerIdentification, i.sale.customerName, count(distinct i.sale), sum(i.lineTotal),
                   sum(i.lineSubtotal), sum(i.quantity * i.unitCost)
            from SaleItem i
            where i.sale.status = com.facturapro.sales.SaleStatus.ISSUED
              and i.sale.saleDate between :from and :to
            group by i.sale.customerIdentification, i.sale.customerName
            order by sum(i.lineTotal) desc
            """)
    List<Object[]> salesByCustomer(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select i.sale.createdBy, i.sale.createdByName, count(distinct i.sale), sum(i.lineTotal),
                   sum(i.lineSubtotal), sum(i.quantity * i.unitCost)
            from SaleItem i
            where i.sale.status = com.facturapro.sales.SaleStatus.ISSUED
              and i.sale.saleDate between :from and :to
            group by i.sale.createdBy, i.sale.createdByName
            order by sum(i.lineTotal) desc
            """)
    List<Object[]> salesByUser(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select i.taxRate, sum(i.lineTax), sum(i.lineSubtotal)
            from SaleItem i
            where i.sale.status = com.facturapro.sales.SaleStatus.ISSUED
              and i.sale.saleDate between :from and :to
            group by i.taxRate
            order by i.taxRate desc
            """)
    List<Object[]> taxesByRate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select s from Sale s
            where s.status = com.facturapro.sales.SaleStatus.ISSUED and s.balance > 0
            order by s.saleDate asc
            """)
    List<com.facturapro.sales.Sale> receivables();

    @Query("""
            select p, coalesce(sum(m.quantity), 0)
            from StockMovement m join m.product p
            group by p
            """)
    List<Object[]> stockFromMovements();

    @Query("""
            select p from Product p
            where p.active = true and p.stock <= p.minStock
            order by p.stock asc
            """)
    List<com.facturapro.products.Product> lowStock();

    @Query("""
            select pu.purchaseDate, count(pu), coalesce(sum(pu.subtotal),0), coalesce(sum(pu.taxTotal),0),
                   coalesce(sum(pu.total),0)
            from Purchase pu
            where pu.status = com.facturapro.purchases.PurchaseStatus.CONFIRMED
              and pu.purchaseDate between :from and :to
            group by pu.purchaseDate
            order by pu.purchaseDate
            """)
    List<Object[]> purchasesByDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Fila unica de agregado. Se declara como List&lt;Object[]&gt; porque el retorno
     * Object[] directo Produce un array anidado [[suma, contador]] en Spring Data.
     */
    @Query("""
            select coalesce(sum(s.total), 0), count(s)
            from Sale s
            where s.status = com.facturapro.sales.SaleStatus.ISSUED and s.saleDate between :from and :to
            """)
    List<Object[]> salesTotals(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(p.amount), 0), count(p)
            from Payment p
            where p.voided = false and p.paymentDate >= :fromInstant and p.paymentDate < :toInstant
            """)
    List<Object[]> paymentsTotals(@Param("fromInstant") java.time.Instant from,
                                  @Param("toInstant") java.time.Instant to);

    @Query("""
            select i.sku, i.productName, count(s), sum(i.quantity), sum(i.lineTotal)
            from SaleItem i join i.sale s
            where s.status = com.facturapro.sales.SaleStatus.ISSUED and s.saleDate between :from and :to
            group by i.sku, i.productName
            order by sum(i.quantity) desc
            """)
    List<Object[]> topProducts(@Param("from") LocalDate from, @Param("to") LocalDate to, org.springframework.data.domain.Pageable pageable);

    @Query("""
            select s from Sale s
            where s.status = com.facturapro.sales.SaleStatus.ISSUED and s.saleDate between :from and :to
            order by s.saleDate desc
            """)
    List<com.facturapro.sales.Sale> issuedSalesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
