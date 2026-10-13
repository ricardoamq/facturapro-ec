package com.facturapro.invoices;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentNumberRepository extends JpaRepository<DocumentNumber, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentNumber d where d.docType = :docType and d.series = :series and d.periodYear = :year")
    Optional<DocumentNumber> findForUpdate(@Param("docType") String docType,
                                           @Param("series") String series,
                                           @Param("year") int year);
}
