package com.facturapro.reports;

import com.facturapro.sales.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Sale, Long>, ReportQueries {
}
