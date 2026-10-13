package com.facturapro.invoices;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_numbers", uniqueConstraints = @UniqueConstraint(name = "uq_document_numbers",
        columnNames = {"doc_type", "series", "period_year"}))
public class DocumentNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_type", nullable = false, length = 20)
    private String docType;

    @Column(nullable = false, length = 20)
    private String series;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "last_number", nullable = false)
    private long lastNumber;
}
