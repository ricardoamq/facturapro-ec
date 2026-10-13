package com.facturapro.settings;

import com.facturapro.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAuthority('SETTINGS_VIEW')")
    public Map<String, String> getAll() {
        return settingsService.getAll();
    }

    @GetMapping("/tax-categories")
    public List<SettingsDtos.TaxCategory> taxCategories() {
        return settingsService.getTaxCategories();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SETTINGS_WRITE')")
    public Map<String, String> update(@RequestBody SettingsDtos.UpdateRequest request) {
        return settingsService.update(request.values());
    }
}
