package com.facturapro.sales;

import com.facturapro.common.error.BusinessException;
import com.facturapro.common.util.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculo unico de importes. El backend SIEMPRE calcula subtotales, descuentos,
 * impuestos y totales; el frontend nunca envia importes definitivos.
 *
 * Regla de redondeo (documentada):
 *  1. bruto     = cantidad * precio unitario
 *  2. descuento = round(bruto * descuento% / 100)        -> escala 2, HALF_UP
 *  3. neto      = round(bruto - descuento)
 *  4. impuesto  = round(neto * tasa% / 100)              -> escala 2, HALF_UP
 *  5. total linea = neto + impuesto
 * El documento suma las lineas YA redondeadas, por lo que el total mostrado
 * siempre coincide exactamente con la suma de las lineas.
 */
@Service
public class PricingService {

    public record LineCalculation(BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total) {
    }

    public record DocumentTotals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal total) {
    }

    public LineCalculation calculateLine(BigDecimal quantity, BigDecimal unitPrice,
                                         BigDecimal discountPercent, BigDecimal taxRate) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException("VALIDATION_ERROR", "La cantidad debe ser mayor que cero.");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new BusinessException("VALIDATION_ERROR", "El precio unitario no puede ser negativo.");
        }
        if (discountPercent != null && (discountPercent.signum() < 0
                || discountPercent.compareTo(new BigDecimal("100")) > 0)) {
            throw new BusinessException("VALIDATION_ERROR", "El descuento debe estar entre 0 y 100.");
        }
        BigDecimal rate = taxRate == null ? BigDecimal.ZERO : taxRate;
        if (rate.signum() < 0) {
            throw new BusinessException("VALIDATION_ERROR", "La tasa de impuesto no puede ser negativa.");
        }
        BigDecimal discount = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal discountAmount = Money.round(gross.multiply(discount).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal net = Money.round(gross.subtract(discountAmount));
        BigDecimal taxAmount = Money.round(net.multiply(rate).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return new LineCalculation(net, discountAmount, taxAmount, net.add(taxAmount));
    }

    public DocumentTotals totals(List<LineCalculation> lines) {
        BigDecimal subtotal = Money.ZERO;
        BigDecimal discount = Money.ZERO;
        BigDecimal tax = Money.ZERO;
        for (LineCalculation line : lines) {
            subtotal = subtotal.add(line.subtotal());
            discount = discount.add(line.discount());
            tax = tax.add(line.tax());
        }
        return new DocumentTotals(subtotal, discount, tax, subtotal.add(tax));
    }
}
