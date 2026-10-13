package com.facturapro.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas unicas de redondeo monetario del sistema.
 *
 * Importes: escala 2, HALF_UP (segun CIFRS de la mayoria de comercios ecuatorianos).
 * Tasas:    escala 4, HALF_UP.
 * El calculo se aplica por linea y el documento suma las lineas ya redondeadas,
 * de modo que el total siempre es la suma exacta de lo mostrado al usuario.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final int RATE_SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(SCALE, ROUNDING);
    }

    public static BigDecimal rate(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(RATE_SCALE, ROUNDING);
    }

    public static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
