package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.dto.MetricsDTO;
import com.assigment.Scheduler.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<MetricsDTO> getMetrics() {
        return ResponseEntity.ok(metricsService.computeAllMetrics());
    }
}
