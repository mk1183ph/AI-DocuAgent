package com.docuagent.localapp.controller;

import com.docuagent.localapp.dto.TabCreateRequest;
import com.docuagent.localapp.dto.TabResponse;
import com.docuagent.localapp.dto.TabUpdateRequest;
import com.docuagent.localapp.service.TabService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tabs")
public class TabController {

    private final TabService tabService;

    public TabController(TabService tabService) {
        this.tabService = tabService;
    }

    @GetMapping
    public List<TabResponse> findAll() {
        return tabService.findAll();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TabResponse> create(@Valid @ModelAttribute TabCreateRequest request) {
        TabResponse response = tabService.create(request);
        return ResponseEntity.created(URI.create("/api/tabs/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public TabResponse findById(@PathVariable Long id) {
        return tabService.findById(id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TabResponse update(@PathVariable Long id, @Valid @ModelAttribute TabUpdateRequest request) {
        return tabService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tabService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
