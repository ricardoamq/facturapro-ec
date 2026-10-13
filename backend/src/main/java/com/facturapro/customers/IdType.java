package com.facturapro.customers;

public enum IdType {
    RUC("RUC"),
    CEDULA("Cedula"),
    PASAPORTE("Pasaporte"),
    IDENTIFICACION_EXTRANJERA("Identificacion extranjera");

    private final String label;

    IdType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
