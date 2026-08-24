package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.entity.Company;
import com.assigment.Scheduler.repository.CompanyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final com.assigment.Scheduler.repository.PanelRepository panelRepository;

    public CompanyController(CompanyRepository companyRepository, com.assigment.Scheduler.repository.PanelRepository panelRepository) {
        this.companyRepository = companyRepository;
        this.panelRepository = panelRepository;
    }

    @GetMapping
    public List<Company> list() {
        return companyRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Company> createOrUpdate(@RequestBody Company company) {
        if (company.getName() != null && !company.getName().trim().isEmpty() && company.getId() == null) {
            companyRepository.findByName(company.getName().trim()).ifPresent(existing -> {
                company.setId(existing.getId());
            });
        }
        if (company.getArrivalDay() == null) {
            company.setArrivalDay(1);
        }
        if (company.getRequiredPanels() != null) {
            company.setMaxPanels(company.getRequiredPanels());
        }
        if (company.getMaxPanels() == null) {
            company.setMaxPanels(1);
        }
        if (company.getTier() == null) {
            company.setTier(com.assigment.Scheduler.entity.CompanyTier.CORE);
        }
        if (company.getCgpaCutoff() == null) {
            company.setCgpaCutoff(0.0);
        }
        Company saved = companyRepository.save(company);
        int reqPanels = Math.max(1, java.util.Optional.ofNullable(saved.getRequiredPanels()).orElse(1));
        List<com.assigment.Scheduler.entity.Panel> existingPanels = panelRepository.findByCompanyId(saved.getId());
        if (existingPanels.size() < reqPanels) {
            for (int i = existingPanels.size() + 1; i <= reqPanels; i++) {
                com.assigment.Scheduler.entity.Panel p = new com.assigment.Scheduler.entity.Panel("Panel " + i + " (" + saved.getName() + ")", saved, "Interviewer 1, Interviewer 2");
                p.setMemberCount(2);
                panelRepository.save(p);
            }
        }
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<?> importCsv(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        java.util.List<java.util.Map<String, Object>> errors = new java.util.ArrayList<>();
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
                    if (p[0].equalsIgnoreCase("name") || p[0].equalsIgnoreCase("companyName") || p[0].equalsIgnoreCase("company")) continue;
                }

                if (p.length < 1 || p[0].isEmpty()) {
                    errors.add(java.util.Map.of("row", row, "reason", "Company name cannot be empty"));
                    continue;
                }

                try {
                    String name = p[0].trim();
                    String tierStr = p.length > 1 && !p[1].isEmpty() ? p[1].trim().toUpperCase() : "CORE";
                    int arrivalDay = p.length > 2 && !p[2].isEmpty() ? Integer.parseInt(p[2].replaceAll("[^0-9]", "")) : 1;
                    double cgpaCutoff = p.length > 3 && !p[3].isEmpty() ? Double.parseDouble(p[3].replace(',', '.')) : 6.0;
                    int duration = p.length > 4 && !p[4].isEmpty() ? Integer.parseInt(p[4].replaceAll("[^0-9]", "")) : 45;
                    int panels = p.length > 5 && !p[5].isEmpty() ? Integer.parseInt(p[5].replaceAll("[^0-9]", "")) : 1;

                    com.assigment.Scheduler.entity.CompanyTier tier;
                    try {
                        tier = com.assigment.Scheduler.entity.CompanyTier.valueOf(tierStr);
                    } catch (Exception e) {
                        tier = com.assigment.Scheduler.entity.CompanyTier.CORE;
                    }

                    Company c = companyRepository.findByName(name).orElse(new Company());
                    c.setName(name);
                    c.setTier(tier);
                    c.setArrivalDay(arrivalDay);
                    c.setCgpaCutoff(cgpaCutoff);
                    c.setInterviewDurationMinutes(duration);
                    c.setRequiredPanels(panels);
                    c.setMaxPanels(panels);
                    if (c.getArrivalTime() == null) c.setArrivalTime("09:00");
                    if (c.getAvailableUntil() == null) c.setAvailableUntil("17:00");
                    companyRepository.save(c);
                    valid++;
                } catch (Exception e) {
                    errors.add(java.util.Map.of("row", row, "reason", "Parse error: " + e.getMessage()));
                }
            }
        }

        return ResponseEntity.ok(java.util.Map.of("valid", valid, "invalid", errors.size(), "errors", errors));
    }
}
