package com.docuagent.localapp.controller;

import com.docuagent.localapp.dto.SettingsResponse;
import com.docuagent.localapp.dto.SettingsUpdateRequest;
import com.docuagent.localapp.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse find() {
        return settingsService.find();
    }

    @PutMapping
    public SettingsResponse update(@Valid @RequestBody SettingsUpdateRequest request) {
        return settingsService.update(request);
    }
}
