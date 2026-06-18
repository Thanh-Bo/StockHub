package com.stockhub.screener.controller;

import com.stockhub.screener.dto.FilterMetadata;
import com.stockhub.screener.dto.ScreenerRequest;
import com.stockhub.screener.dto.ScreenerResponse;
import com.stockhub.screener.service.ScreenerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/screener")
public class ScreenerController {

    private final ScreenerService screenerService;

    public ScreenerController(ScreenerService screenerService) {
        this.screenerService = screenerService;
    }

    @PostMapping("/search")
    public ResponseEntity<ScreenerResponse> search(@Valid @RequestBody ScreenerRequest request) {
        ScreenerResponse response = screenerService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filters")
    public ResponseEntity<List<FilterMetadata>> getFilters() {
        List<FilterMetadata> filters = screenerService.getAvailableFilters();
        return ResponseEntity.ok(filters);
    }
}
