package com.facturapro.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public ReportService.SalesSummary sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesSummary(from, to);
    }

    @GetMapping("/sales/by-product")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public List<ReportService.ProductRow> byProduct(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesByProduct(from, to);
    }

    @GetMapping("/sales/by-customer")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public List<ReportService.PartyRow> byCustomer(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesByCustomer(from, to);
    }

    @GetMapping("/sales/by-user")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public List<ReportService.PartyRow> byUser(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.salesByUser(from, to);
    }

    @GetMapping("/sales/export")
    @PreAuthorize("hasAuthority('REPORTS_EXPORT')")
    public ResponseEntity<byte[]> exportSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<String> lines = reportService.exportSalesCsv(from, to);
        String csv = "﻿" + String.join("\r\n", lines);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ventas_" + from + "_" + to + ".csv\"")
                .contentType(new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8))
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/taxes")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public List<ReportService.TaxRow> taxes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.taxes(from, to);
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public List<ReportService.ReceivableRow> receivables() {
        return reportService.receivables();
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public List<ReportService.InventoryRow> inventory() {
        return reportService.inventory();
    }

    @GetMapping("/purchases")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public ReportService.PurchasesSummary purchases(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.purchases(from, to);
    }
}
