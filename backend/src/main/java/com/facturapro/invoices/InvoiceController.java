package com.facturapro.invoices;

import com.facturapro.sales.Sale;
import com.facturapro.sales.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final SaleService saleService;
    private final InvoicePdfService pdfService;

    @GetMapping("/{number}/pdf")
    @PreAuthorize("hasAuthority('SALES_VIEW')")
    public ResponseEntity<byte[]> pdf(@PathVariable String number) throws Exception {
        Sale sale = saleService.byNumberEntity(number);
        byte[] pdf = pdfService.renderPdf(sale);
        String filename = (sale.getNumber() == null ? "borrador-" + sale.getId() : sale.getNumber()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{number}/html")
    @PreAuthorize("hasAuthority('SALES_VIEW')")
    public ResponseEntity<String> html(@PathVariable String number) {
        Sale sale = saleService.byNumberEntity(number);
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(pdfService.renderHtml(sale));
    }
}
