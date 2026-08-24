package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.entity.*;
import com.assigment.Scheduler.repository.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final RoomRepository rooms;
    private final PanelRepository panels;
    private final CompanyRepository companies;
    private final ResourceAvailabilityRepository availability;
    private final InterviewRepository interviews;

    public ResourceController(RoomRepository rooms, PanelRepository panels, CompanyRepository companies,
            ResourceAvailabilityRepository availability, InterviewRepository interviews) {
        this.rooms = rooms;
        this.panels = panels;
        this.companies = companies;
        this.availability = availability;
        this.interviews = interviews;
    }

    @GetMapping("/rooms")
    public List<Room> rooms() {
        return rooms.findAll();
    }

    @PostMapping("/rooms")
    public Room createRoom(@RequestBody Room r) {
        r.setIsActive(r.getIsActive() == null || r.getIsActive());
        return rooms.save(r);
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody Room r) {
        return rooms.findById(id).map(existing -> {
            if (r.getRoomNumber() != null) existing.setRoomNumber(r.getRoomNumber());
            if (r.getBuilding() != null) existing.setBuilding(r.getBuilding());
            if (r.getCapacity() != null) existing.setCapacity(r.getCapacity());
            if (r.getIsActive() != null) existing.setIsActive(r.getIsActive());
            rooms.save(existing);
            return ResponseEntity.ok(existing);
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        if (!rooms.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // 1. Unbind room from interviews to prevent Foreign Key constraint violation
        List<Interview> ivs = interviews.findByRoomId(id);
        for (Interview iv : ivs) {
            iv.setRoom(null);
            if (iv.getStatus() == InterviewStatus.SCHEDULED || iv.getStatus() == InterviewStatus.MOVED) {
                iv.setStatus(InterviewStatus.REPLAN_REQUIRED);
                iv.setUnscheduledReason(UnscheduledReason.ROOM_EXHAUSTED);
            }
        }
        interviews.saveAll(ivs);

        // 2. Delete availability entries for room
        try {
            availability.deleteByResourceTypeAndResourceId("ROOM", id);
        } catch (Exception ignored) {}

        // 3. Delete room
        rooms.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/panels/{id}")
    public ResponseEntity<?> deletePanel(@PathVariable Long id) {
        if (!panels.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<Interview> ivs = interviews.findByPanelId(id);
        for (Interview iv : ivs) {
            iv.setPanel(null);
            if (iv.getStatus() == InterviewStatus.SCHEDULED || iv.getStatus() == InterviewStatus.MOVED) {
                iv.setStatus(InterviewStatus.REPLAN_REQUIRED);
                iv.setUnscheduledReason(UnscheduledReason.PANEL_UNAVAILABLE);
            }
        }
        interviews.saveAll(ivs);

        try {
            availability.deleteByResourceTypeAndResourceId("PANEL", id);
        } catch (Exception ignored) {}

        panels.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/panels")
    public List<Panel> panels() {
        return panels.findAll();
    }

    @PostMapping("/panels")
    public ResponseEntity<?> createPanel(@RequestBody Map<String, Object> body) {
        Object cidObj = body.get("companyId");
        if (cidObj == null || cidObj.toString().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Company ID is required");
        }
        Long companyId;
        try {
            companyId = Long.valueOf(cidObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid company ID format");
        }
        Company c = companies.findById(companyId).orElse(null);
        if (c == null)
            return ResponseEntity.badRequest().body("Unknown companyId");
        Panel p = new Panel(String.valueOf(body.get("name")), c,
                String.valueOf(body.getOrDefault("interviewerNames", "")));
        p.setMemberCount(Integer.valueOf(body.getOrDefault("memberCount", 2).toString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(panels.save(p));
    }

    @GetMapping("/availability")
    public List<ResourceAvailability> availability() {
        return availability.findAll();
    }

    @PostMapping("/availability")
    public ResourceAvailability createAvailability(@RequestBody ResourceAvailability a) {
        return availability.save(a);
    }

    @PostMapping(value = "/panels/import", consumes = "multipart/form-data")
    public ResponseEntity<?> importPanelsCsv(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        List<Map<String, Object>> errors = new ArrayList<>();
        int valid = 0;
        int row = 0;

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                row++;
                String cleanedLine = line.replace("\uFEFF", "").replace("\r", "").trim();
                if (cleanedLine.isEmpty()) continue;

                String[] p = cleanedLine.split(",", -1);
                for (int i = 0; i < p.length; i++) {
                    p[i] = p[i].trim().replaceAll("^\"|\"$", "");
                }

                if (firstLine) {
                    firstLine = false;
                    if (p[0].equalsIgnoreCase("name") || p[0].equalsIgnoreCase("panelName")) continue;
                }

                if (p.length < 2 || p[0].isEmpty()) {
                    errors.add(Map.of("row", row, "reason", "Expected panel name and company name/ID"));
                    continue;
                }

                try {
                    String name = p[0].trim();
                    String compRef = p[1].trim();
                    int members = p.length > 2 && !p[2].isEmpty() ? Integer.parseInt(p[2].replaceAll("[^0-9]", "")) : 2;
                    String interviewers = p.length > 3 ? p[3].trim() : "";

                    Company c = null;
                    if (compRef.matches("\\d+")) {
                        c = companies.findById(Long.valueOf(compRef)).orElse(null);
                    }
                    if (c == null) {
                        c = companies.findByName(compRef).orElse(null);
                    }
                    if (c == null) {
                        errors.add(Map.of("row", row, "reason", "Company not found for '" + compRef + "'"));
                        continue;
                    }

                    Panel panel = new Panel(name, c, interviewers);
                    panel.setMemberCount(members);
                    panels.save(panel);
                    valid++;
                } catch (Exception e) {
                    errors.add(Map.of("row", row, "reason", "Parse error: " + e.getMessage()));
                }
            }
        }

        return ResponseEntity.ok(Map.of("valid", valid, "invalid", errors.size(), "errors", errors));
    }

    @PostMapping(value = "/rooms/import", consumes = "multipart/form-data")
    public ResponseEntity<?> importRoomsCsv(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        List<Map<String, Object>> errors = new ArrayList<>();
        int valid = 0;
        int row = 0;

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                row++;
                String cleanedLine = line.replace("\uFEFF", "").replace("\r", "").trim();
                if (cleanedLine.isEmpty()) continue;

                String[] p = cleanedLine.split(",", -1);
                for (int i = 0; i < p.length; i++) {
                    p[i] = p[i].trim().replaceAll("^\"|\"$", "");
                }

                if (firstLine) {
                    firstLine = false;
                    if (p[0].equalsIgnoreCase("roomNumber") || p[0].equalsIgnoreCase("room")) continue;
                }

                if (p.length < 1 || p[0].isEmpty()) {
                    errors.add(Map.of("row", row, "reason", "Room number cannot be empty"));
                    continue;
                }

                try {
                    String roomNumber = p[0].trim();
                    String building = p.length > 1 && !p[1].isEmpty() ? p[1].trim() : "Main Block";
                    int capacity = p.length > 2 && !p[2].isEmpty() ? Integer.parseInt(p[2].replaceAll("[^0-9]", "")) : 1;

                    Room room = new Room(roomNumber, building, capacity);
                    room.setIsActive(true);
                    rooms.save(room);
                    valid++;
                } catch (Exception e) {
                    errors.add(Map.of("row", row, "reason", "Parse error: " + e.getMessage()));
                }
            }
        }

        return ResponseEntity.ok(Map.of("valid", valid, "invalid", errors.size(), "errors", errors));
    }
}
