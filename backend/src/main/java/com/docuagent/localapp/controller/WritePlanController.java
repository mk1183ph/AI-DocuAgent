package com.docuagent.localapp.controller;

import com.docuagent.localapp.dto.WritePlanResponse;
import com.docuagent.localapp.service.WritePlanService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class WritePlanController {

    private final WritePlanService writePlanService;

    public WritePlanController(WritePlanService writePlanService) {
        this.writePlanService = writePlanService;
    }

    @GetMapping("/{taskId}/write-plan")
    public List<WritePlanResponse> plan(@PathVariable Long taskId) {
        return writePlanService.plan(taskId);
    }
}
