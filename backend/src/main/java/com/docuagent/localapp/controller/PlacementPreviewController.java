package com.docuagent.localapp.controller;

import com.docuagent.localapp.dto.PlacementPreviewResponse;
import com.docuagent.localapp.service.PlacementPreviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class PlacementPreviewController {

    private final PlacementPreviewService placementPreviewService;

    public PlacementPreviewController(PlacementPreviewService placementPreviewService) {
        this.placementPreviewService = placementPreviewService;
    }

    @GetMapping("/{taskId}/placement-preview")
    public List<PlacementPreviewResponse> preview(@PathVariable Long taskId) {
        return placementPreviewService.preview(taskId);
    }
}
