package com.facturapro.common.util;

/**
 * Validacion de formato de identificacion ecuatoriana basada en algoritmos publicos
 * del Registro Civil / SRI (modulo 10 para cedula y RUC de personas naturales;
 * digito verificador modulo 11 para RUC juridico).
 *
 * LIMITACION DOCUMENTADA: esta validacion es LOCAL y de FORMATO. No consulta los
 * servicios del SRI ni garantiza que la identificacion este activa ni corresponda
 * al nombre registrado. Para validar contra el SRI se requiere integracion oficial.
 */
public final class IdentificationValidator {

    private IdentificationValidator() {
    }

    public static boolean isValidCedula(String value) {
        if (value == null || !value.matches("\\d{10}")) {
            return false;
        }
        int province = Integer.parseInt(value.substring(0, 2));
        if (province < 1 || province > 24) {
            return false;
        }
        if (value.charAt(2) >= '6') {
            return false;
        }
        return modulo10(value.substring(0, 9)) == Character.getNumericValue(value.charAt(9));
    }

    public static boolean isValidRuc(String value) {
        if (value == null || !value.matches("\\d{13}")) {
            return false;
        }
        if (value.startsWith("0000")) {
            // RUC de entidades publicas: formato basico.
            return true;
        }
        int third = Character.getNumericValue(value.charAt(2));
        if (third < 6) {
            return isValidCedula(value.substring(0, 10)) && value.endsWith("001");
        }
        return modulo11Ruc(value) == Character.getNumericValue(value.charAt(12));
    }

    public static boolean isValidPassport(String value) {
        return value != null && value.matches("[A-Za-z0-9]{5,20}");
    }

    public static boolean isValidForeignId(String value) {
        return value != null && value.matches("[A-Za-z0-9\\-]{5,20}");
    }

    public static boolean isValid(String identification, String idType) {
        if (idType == null) {
            return identification != null && !identification.isBlank();
        }
        return switch (idType) {
            case "CEDULA" -> isValidCedula(identification);
            case "RUC" -> isValidRuc(identification);
            case "PASAPORTE" -> isValidPassport(identification);
            case "IDENTIFICACION_EXTRANJERA" -> isValidForeignId(identification);
            default -> false;
        };
    }

    private static int modulo10(String digits) {
        int[] coefficients = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int value = Character.getNumericValue(digits.charAt(i)) * coefficients[i];
            if (value > 9) {
                value -= 9;
            }
            sum += value;
        }
        int nextTen = ((sum / 10) + 1) * 10;
        return (nextTen - sum) % 10;
    }

    private static int modulo11Ruc(String digits) {
        int[] coefficients = {3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < coefficients.length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * coefficients[i];
        }
        int remainder = sum % 11;
        return remainder == 0 ? 0 : (11 - remainder) % 10;
    }
}
