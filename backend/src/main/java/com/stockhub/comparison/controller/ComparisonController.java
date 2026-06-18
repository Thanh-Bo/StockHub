package com.stockhub.comparison.controller;

import com.stockhub.comparison.dto.ComparisonRequest;
import com.stockhub.comparison.dto.ComparisonResponse;
import com.stockhub.comparison.service.ComparisonService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @PostMapping("/compare")
    public ResponseEntity<ComparisonResponse> compare(@Valid @RequestBody ComparisonRequest request) {
        ComparisonResponse response = comparisonService.compare(request);
        return ResponseEntity.ok(response);
    }
}
