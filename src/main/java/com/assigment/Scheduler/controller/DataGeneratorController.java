package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.dto.SeedRequest;
import com.assigment.Scheduler.dto.SeedResponse;
import com.assigment.Scheduler.service.DataGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
public class DataGeneratorController {

    private final DataGeneratorService dataGeneratorService;

    public DataGeneratorController(DataGeneratorService dataGeneratorService) {
        this.dataGeneratorService = dataGeneratorService;
    }

    @PostMapping("/seed")
    public ResponseEntity<SeedResponse> seed(@RequestBody(required = false) SeedRequest request) {
        if (request == null) request = new SeedRequest();
        try {
            SeedResponse resp = dataGeneratorService.seedDatabase(request);
            resp.setMessage("Database seeded successfully. Ready to run initial scheduling.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            SeedResponse err = new SeedResponse();
            err.setStatus("FAILED");
            err.setMessage("Error during seed: " + e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @DeleteMapping("/reset")
    public ResponseEntity<SeedResponse> reset() {
        try {
            dataGeneratorService.clearAllData();
            SeedResponse resp = new SeedResponse();
            resp.setStatus("SUCCESS");
            resp.setMessage("All data cleared.");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            SeedResponse err = new SeedResponse();
            err.setStatus("FAILED");
            err.setMessage("Error during reset: " + e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }
}
