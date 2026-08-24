package com.assigment.Scheduler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        String dbStatus = "DOWN";
        try (Connection c = dataSource.getConnection()) {
            if (c.isValid(2)) dbStatus = "UP";
        } catch (Exception ignore) {}
        return ResponseEntity.ok(Map.of(
            "status", dbStatus.equals("UP") ? "UP" : "DEGRADED",
            "database", dbStatus,
            "timestamp", System.currentTimeMillis()
        ));
    }
}
