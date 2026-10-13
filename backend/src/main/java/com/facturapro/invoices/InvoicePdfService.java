package com.facturapro.invoices;

import com.facturapro.sales.Sale;
import com.facturapro.sales.SaleItem;
import com.facturapro.sales.SaleStatus;
import com.facturapro.settings.SettingsService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Genera el PDF del comprobante interno de venta a partir de la instantanea
 * almacenada en la base de datos (no del catalogo actual).
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SettingsService settingsService;

    public byte[] renderPdf(Sale sale) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> openFont("fonts/DejaVuSans.ttf"), "DejaVu");
            builder.useFont(() -> openFont("fonts/DejaVuSans-Bold.ttf"), "DejaVu", 700, null, true);
            builder.withHtmlContent(renderHtml(sale), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    private InputStream openFont(String path) {
        try {
            return new ClassPathResource(path).getInputStream();
        } catch (java.io.IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        }
    }

    public String renderHtml(Sale sale) {
        String businessName = settingsService.get("business.name");
        String legalName = settingsService.get("business.legalName");
        String ruc = settingsService.get("business.ruc");
        String address = settingsService.get("business.address");
        String phone = settingsService.get("business.phone");
        String email = settingsService.get("business.email");
        String currencySymbol = settingsService.get("currency.symbol");

        StringBuilder rows = new StringBuilder();
        int index = 1;
        for (SaleItem item : sale.getItems()) {
            rows.append("""
                    <tr>
                      <td class="c">%d</td>
                      <td>%s<br/><span class="muted">SKU: %s</span></td>
                      <td class="c">%s</td>
                      <td class="r">%s</td>
                      <td class="r">%s%%</td>
                      <td class="r">%s</td>
                      <td class="r">%s %s</td>
                    </tr>
                    """.formatted(
                    index++,
                    escape(item.getProductName()),
                    escape(item.getSku()),
                    item.getQuantity().stripTrailingZeros().toPlainString(),
                    money(item.getUnitPrice(), currencySymbol),
                    item.getDiscountPercent().stripTrailingZeros().toPlainString(),
                    item.getTaxRate().stripTrailingZeros().toPlainString(),
                    currencySymbol, moneyPlain(item.getLineTotal())));
        }

        String voidBadge = sale.getStatus() == SaleStatus.VOIDED
                ? "<div class=\"void-badge\">ANULADO</div>"
                : "";

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    @page { size: A4; margin: 18mm 14mm; }
                    body { font-family: DejaVu, sans-serif; font-size: 11px; color: #1f2937; }
                    h1 { font-size: 18px; margin: 0 0 2px 0; }
                    .header { display: flex; justify-content: space-between; border-bottom: 2px solid #0f766e; padding-bottom: 10px; margin-bottom: 14px; }
                    .box { border: 1px solid #d1d5db; border-radius: 6px; padding: 10px; width: 48%; }
                    .box h2 { font-size: 12px; margin: 0 0 6px 0; text-transform: uppercase; color: #0f766e; }
                    .muted { color: #6b7280; font-size: 10px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 14px; }
                    th { background: #f0fdfa; color: #115e59; text-align: left; padding: 6px; border-bottom: 1px solid #99f6e4; font-size: 10px; text-transform: uppercase; }
                    td { padding: 6px; border-bottom: 1px solid #e5e7eb; vertical-align: top; }
                    .c { text-align: center; } .r { text-align: right; }
                    .totals { margin-top: 14px; margin-left: auto; width: 45%; }
                    .totals td { padding: 4px 6px; }
                    .totals .grand td { font-weight: bold; font-size: 13px; border-top: 2px solid #0f766e; }
                    .void-badge { margin-top: 10px; color: #b91c1c; font-size: 22px; font-weight: bold; text-align: center; border: 2px solid #b91c1c; padding: 6px; }
                    .footer { margin-top: 22px; font-size: 10px; color: #6b7280; border-top: 1px solid #e5e7eb; padding-top: 8px; }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <div>
                      <h1>%s</h1>
                      <div>%s</div>
                      <div class="muted">RUC: %s</div>
                      <div class="muted">%s</div>
                      <div class="muted">Tel: %s | %s</div>
                    </div>
                    <div style="text-align:right">
                      <div style="font-size:15px;font-weight:bold;color:#0f766e">COMPROBANTE INTERNO DE VENTA</div>
                      <div>N° <b>%s</b></div>
                      <div>Fecha: %s</div>
                      <div>Estado: %s</div>
                      <div>Atendido por: %s</div>
                    </div>
                  </div>
                  %s
                  <div style="display:flex;gap:12px">
                    <div class="box">
                      <h2>Cliente</h2>
                      <div><b>%s</b></div>
                      <div>Identificacion: %s</div>
                      <div class="muted">Direccion: %s</div>
                      <div class="muted">Correo: %s</div>
                    </div>
                    <div class="box">
                      <h2>Documento</h2>
                      <div>Tipo: Factura comercial interna</div>
                      <div>SRI: <b>No autorizado electr&oacute;nicamente</b></div>
                      <div>Moneda: D&oacute;lares de los EE.UU. (USD)</div>
                      <div class="muted">Este documento no reemplaza al comprobante autorizado por el SRI.</div>
                    </div>
                  </div>
                  <table>
                    <thead>
                      <tr>
                        <th class="c">#</th><th>Producto</th><th class="c">Cant.</th>
                        <th class="r">Precio</th><th class="r">Dcto.</th><th class="r">IVA</th><th class="r">Total</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                  <table class="totals">
                    <tr><td>Subtotal</td><td class="r">%s %s</td></tr>
                    <tr><td>Descuentos</td><td class="r">%s %s</td></tr>
                    <tr><td>IVA</td><td class="r">%s %s</td></tr>
                    <tr class="grand"><td>TOTAL</td><td class="r">%s %s</td></tr>
                    <tr><td>Pagado</td><td class="r">%s %s</td></tr>
                    <tr><td>Saldo</td><td class="r">%s %s</td></tr>
                  </table>
                  <div class="footer">
                    Generado por FacturaPro EC el %s. Documento emitido a partir de los datos registrados al momento de la venta;
                    cambios posteriores del catalogo no lo modifican.
                  </div>
                </body>
                </html>
                """.formatted(
                escape(businessName),
                escape(legalName),
                escape(ruc == null || ruc.isBlank() ? "—" : ruc),
                escape(address == null || address.isBlank() ? "—" : address),
                escape(phone == null || phone.isBlank() ? "—" : phone),
                escape(email == null || email.isBlank() ? "—" : email),
                escape(sale.getNumber() == null ? "BORRADOR-" + sale.getId() : sale.getNumber()),
                sale.getSaleDate().format(DATE),
                statusLabel(sale),
                escape(sale.getCreatedByName() == null ? "—" : sale.getCreatedByName()),
                voidBadge,
                escape(sale.getCustomerName() == null ? "Consumidor final" : sale.getCustomerName()),
                escape(sale.getCustomerIdentification() == null ? "9999999999999" : sale.getCustomerIdentification()),
                escape(sale.getCustomerAddress() == null ? "—" : sale.getCustomerAddress()),
                escape(sale.getCustomerEmail() == null ? "—" : sale.getCustomerEmail()),
                rows,
                currencySymbol, moneyPlain(sale.getSubtotal()),
                currencySymbol, moneyPlain(sale.getDiscountTotal()),
                currencySymbol, moneyPlain(sale.getTaxTotal()),
                currencySymbol, moneyPlain(sale.getTotal()),
                currencySymbol, moneyPlain(sale.getAmountPaid()),
                currencySymbol, moneyPlain(sale.getBalance()),
                DATE_TIME.format(java.time.LocalDateTime.ofInstant(
                        sale.getIssuedAt() == null ? sale.getCreatedAt() : sale.getIssuedAt(), ECUADOR)));
    }

    private String statusLabel(Sale sale) {
        return switch (sale.getStatus()) {
            case DRAFT -> "Borrador";
            case ISSUED -> "Emitido";
            case VOIDED -> "Anulado";
        };
    }

    private String money(java.math.BigDecimal value, String symbol) {
        return symbol + " " + moneyPlain(value);
    }

    private String moneyPlain(java.math.BigDecimal value) {
        return value == null ? "0.00" : String.format("%,.2f", value).replace(',', '\u00A0');
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
