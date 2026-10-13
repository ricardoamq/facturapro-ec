package com.facturapro.settings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record SettingsDtos() {

    public record TaxCategory(String code, String name, BigDecimal rate) {
    }

    public record UpdateRequest(@NotNull @Size(max = 100) Map<
            @jakarta.validation.constraints.Size(max = 80) String,
            @jakarta.validation.constraints.Size(max = 4000) String> values) {
    }
}
