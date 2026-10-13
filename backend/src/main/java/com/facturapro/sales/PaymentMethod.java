package com.facturapro.sales;

public enum PaymentMethod {
    CASH("Efectivo"),
    CARD("Tarjeta"),
    TRANSFER("Transferencia"),
    CHECK("Cheque"),
    OTHER("Otro");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
