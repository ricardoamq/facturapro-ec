package com.facturapro.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.facturapro.audit.AuditService;
import com.facturapro.common.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    /** Claves administrables desde Configuracion. Cualquier otra clave se ignora. */
    public static final Set<String> ALLOWED_KEYS = Set.of(
            "business.name", "business.legalName", "business.ruc", "business.address",
            "business.phone", "business.email", "business.logoBase64",
            "currency.code", "currency.symbol",
            "locale.datePattern", "locale.dateTimePattern",
            "tax.categories", "tax.defaultCategory",
            "numbering.saleSeries", "numbering.purchaseSeries", "numbering.startAt",
            "sales.allowDrafts", "sales.allowPartialPayments",
            "inventory.allowNegativeStock", "inventory.lowStockEnabled");

    private final SettingRepository repository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Map<String, String> getAll() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : ALLOWED_KEYS) {
            result.put(key, get(key));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public String get(String key) {
        return cache.computeIfAbsent(key, k -> repository.findByKey(k)
                .map(Setting::getValue)
                .orElse(defaultFor(k)));
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTaxRate(String categoryCode) {
        return getTaxCategories().stream()
                .filter(c -> c.code().equalsIgnoreCase(categoryCode))
                .findFirst()
                .map(SettingsDtos.TaxCategory::rate)
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR",
                        "Categoria de impuesto desconocida: " + categoryCode));
    }

    @Transactional(readOnly = true)
    public List<SettingsDtos.TaxCategory> getTaxCategories() {
        try {
            return objectMapper.readValue(get("tax.categories"), new TypeReference<List<SettingsDtos.TaxCategory>>() {
            });
        } catch (Exception ex) {
            log.error("No se pudo interpretar tax.categories, usando GENERAL=15%", ex);
            return List.of(new SettingsDtos.TaxCategory("EXEMPT", "Exento", BigDecimal.ZERO),
                    new SettingsDtos.TaxCategory("GENERAL", "General", new BigDecimal("15")),
                    new SettingsDtos.TaxCategory("REDUCED", "Reducido", new BigDecimal("5")));
        }
    }

    @Transactional
    public Map<String, String> update(Map<String, String> values) {
        Map<String, String> applied = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            if (!ALLOWED_KEYS.contains(key)) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue();
            validate(key, value);
            Setting setting = repository.findByKey(key).orElseGet(() -> {
                Setting s = new Setting();
                s.setKey(key);
                return s;
            });
            setting.setValue(value);
            repository.save(setting);
            cache.put(key, value);
            applied.put(key, value);
        }
        auditService.record("SETTINGS_UPDATED", "SETTINGS", null,
                "Claves actualizadas: " + String.join(", ", applied.keySet()));
        return getAll();
    }

    private void validate(String key, String value) {
        switch (key) {
            case "tax.categories" -> {
                try {
                    List<SettingsDtos.TaxCategory> categories =
                            objectMapper.readValue(value, new TypeReference<>() {
                            });
                    if (categories.isEmpty()) {
                        throw new BusinessException("VALIDATION_ERROR", "Debe existir al menos una categoria de impuesto.");
                    }
                    for (SettingsDtos.TaxCategory c : categories) {
                        if (c.code() == null || c.rate() == null
                                || c.rate().compareTo(BigDecimal.ZERO) < 0
                                || c.rate().compareTo(new BigDecimal("100")) > 0) {
                            throw new BusinessException("VALIDATION_ERROR",
                                    "Categoria de impuesto invalida: " + (c.code() == null ? "(sin codigo)" : c.code()));
                        }
                    }
                } catch (BusinessException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new BusinessException("VALIDATION_ERROR", "La configuracion de impuestos no es un JSON valido.");
                }
            }
            case "numbering.startAt" -> {
                try {
                    if (Long.parseLong(value) < 1) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new BusinessException("VALIDATION_ERROR", "El numero inicial debe ser un entero positivo.");
                }
            }
            case "tax.defaultCategory" -> {
                boolean exists = getTaxCategories().stream().anyMatch(c -> c.code().equalsIgnoreCase(value));
                if (!exists) {
                    throw new BusinessException("VALIDATION_ERROR", "La categoria de impuesto por defecto no existe.");
                }
            }
            default -> {
                if (value.length() > 4000) {
                    throw new BusinessException("VALIDATION_ERROR", "Valor demasiado largo para " + key);
                }
            }
        }
    }

    private String defaultFor(String key) {
        return switch (key) {
            case "currency.code" -> "USD";
            case "currency.symbol" -> "$";
            case "locale.datePattern" -> "dd/MM/yyyy";
            case "locale.dateTimePattern" -> "dd/MM/yyyy HH:mm";
            case "tax.defaultCategory" -> "GENERAL";
            case "numbering.saleSeries" -> "FAC";
            case "numbering.purchaseSeries" -> "COM";
            case "numbering.startAt" -> "1";
            case "sales.allowDrafts", "sales.allowPartialPayments", "inventory.lowStockEnabled" -> "true";
            case "inventory.allowNegativeStock" -> "false";
            case "tax.categories" -> """
                    [{"code":"EXEMPT","name":"Exento","rate":0},{"code":"GENERAL","name":"General","rate":15},{"code":"REDUCED","name":"Reducido (materiales de construccion)","rate":5}]""";
            default -> "";
        };
    }

    @Transactional
    public void evictCache() {
        cache.clear();
    }
}
