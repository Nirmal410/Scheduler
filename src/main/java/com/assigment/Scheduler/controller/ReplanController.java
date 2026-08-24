package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.dto.ReplanConfirmResponse;
import com.assigment.Scheduler.dto.ReplanDiffDTO;
import com.assigment.Scheduler.dto.ReplanRequest;
import com.assigment.Scheduler.service.ReplanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/replan")
public class ReplanController {

    private final ReplanningService replanningService;

    public ReplanController(ReplanningService replanningService) {
        this.replanningService = replanningService;
    }

    @PostMapping("/preview")
    public ResponseEntity<ReplanDiffDTO> previewReplan(@RequestBody ReplanRequest request) {
        ReplanDiffDTO resp = replanningService.previewReplan(request.getDisruptionId());
        if ("DISRUPTION_NOT_FOUND".equals(resp.getStatus()))
            return ResponseEntity.status(404).body(resp);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/preview-exception")
    public ResponseEntity<ReplanDiffDTO> previewException(@RequestBody ReplanRequest request) {
        ReplanDiffDTO response = replanningService.previewException(request.getInterviewId());
        if ("INTERVIEW_NOT_FOUND".equals(response.getStatus()))
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ReplanConfirmResponse> confirmReplan(@RequestBody ReplanRequest request) {
        ReplanConfirmResponse resp = replanningService.confirmReplan(
                request.getDisruptionId(), request.getSnapshotId(), request.getOptionId());
        if ("DISRUPTION_NOT_FOUND".equals(resp.getStatus()) || "SNAPSHOT_EXPIRED".equals(resp.getStatus())) {
            return ResponseEntity.status(404).body(resp);
        }
        if ("OPTION_NOT_FOUND".equals(resp.getStatus())) {
            return ResponseEntity.badRequest().body(resp);
        }
        // PARTIALLY_COMMITTED and COMMITTED are both 200 OK
        return ResponseEntity.ok(resp);
    }

    /**
     * Coordinator explicitly authorizes cross-day movement for the remaining interviews
     * that could not be repaired on the same day.
     * This is a separate endpoint to make the authorization intentional and auditable.
     */
    @PostMapping("/confirm-cross-day")
    public ResponseEntity<ReplanConfirmResponse> confirmCrossDay(@RequestBody ReplanRequest request) {
        ReplanConfirmResponse resp = replanningService.confirmReplan(
                request.getDisruptionId(), request.getSnapshotId(), "ALLOW_CROSS_DAY");
        if ("SNAPSHOT_EXPIRED".equals(resp.getStatus())) {
            return ResponseEntity.status(404).body(resp);
        }
        if ("OPTION_NOT_FOUND".equals(resp.getStatus())) {
            return ResponseEntity.badRequest().body(resp);
        }
        return ResponseEntity.ok(resp);
    }
}
