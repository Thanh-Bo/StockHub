package com.stockhub.company.controller;

import com.stockhub.company.dto.CompanySearchResponse;
import com.stockhub.company.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<List<CompanySearchResponse>> fullSearch(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "10") int limit) {
        // Record the search for trending
        searchService.recordSearch(q);
        List<CompanySearchResponse> results = searchService.fullSearch(q, limit);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<CompanySearchResponse>> autocomplete(
            @RequestParam("q") String q) {
        List<CompanySearchResponse> results = searchService.autocomplete(q, 8);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<String>> getTrending() {
        List<String> trending = searchService.getTrendingSearches(10);
        return ResponseEntity.ok(trending);
    }
}
