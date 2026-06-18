package com.stockhub.comparison.controller;

import com.stockhub.comparison.dto.IndustryAveragesResponse;
import com.stockhub.comparison.service.IndustryService;
import com.stockhub.company.entity.Industry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/industries")
public class IndustryController {

    private final IndustryService industryService;

    public IndustryController(IndustryService industryService) {
        this.industryService = industryService;
    }

    @GetMapping
    public ResponseEntity<List<Industry>> getAllIndustries() {
        List<Industry> industries = industryService.getAllIndustries();
        return ResponseEntity.ok(industries);
    }

    @GetMapping("/{sector}/{industry}/averages")
    public ResponseEntity<IndustryAveragesResponse> getAverages(
            @PathVariable String sector,
            @PathVariable String industry) {
        IndustryAveragesResponse response = industryService.getAverages(sector, industry);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
