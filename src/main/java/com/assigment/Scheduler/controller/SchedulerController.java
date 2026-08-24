package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.dto.InterviewDTO;
import com.assigment.Scheduler.dto.ScheduleResultDTO;
import com.assigment.Scheduler.dto.UnscheduledDTO;
import com.assigment.Scheduler.service.SchedulingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulingService schedulingService;

    public SchedulerController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> runSchedule() {
        try {
            ScheduleResultDTO result = schedulingService.runInitialSchedule();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error running scheduler: " + e.getMessage()));
        }
    }

    @GetMapping("/schedule")
    public ResponseEntity<List<InterviewDTO>> getSchedule(
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(schedulingService.getSchedule(day, companyId, studentId, status));
    }

    @GetMapping("/unscheduled")
    public ResponseEntity<List<UnscheduledDTO>> getUnscheduled() {
        return ResponseEntity.ok(schedulingService.getUnscheduled());
    }
}
