package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.dto.DisruptionDTO;
import com.assigment.Scheduler.dto.DisruptionRequest;
import com.assigment.Scheduler.dto.DisruptionResponse;
import com.assigment.Scheduler.service.ReplanningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disruptions")
public class DisruptionController {

    private final ReplanningService replanningService;

    public DisruptionController(ReplanningService replanningService) {
        this.replanningService = replanningService;
    }

    @PostMapping
    public ResponseEntity<DisruptionResponse> createDisruption(@RequestBody DisruptionRequest request) {
        try {
            DisruptionResponse resp = replanningService.createDisruption(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            DisruptionResponse err = new DisruptionResponse();
            err.setStatus("ERROR: " + e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @GetMapping
    public ResponseEntity<List<DisruptionDTO>> listDisruptions() {
        return ResponseEntity.ok(replanningService.getAllDisruptions());
    }
}
