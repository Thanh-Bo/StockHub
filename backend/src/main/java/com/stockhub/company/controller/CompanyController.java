package com.stockhub.company.controller;

import com.stockhub.common.dto.PagedResponse;
import com.stockhub.company.dto.CompanyResponse;
import com.stockhub.company.dto.CompanySummaryResponse;
import com.stockhub.company.dto.DashboardResponse;
import com.stockhub.company.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<CompanyResponse> getCompanyProfile(@PathVariable String ticker) {
        CompanyResponse response = companyService.getCompanyProfile(ticker);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<CompanySummaryResponse>> listCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PagedResponse<CompanySummaryResponse> response = companyService.listCompanies(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticker}/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable String ticker) {
        DashboardResponse response = companyService.getDashboard(ticker);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ticker}/profile")
    public ResponseEntity<CompanyResponse> getProfile(@PathVariable String ticker) {
        CompanyResponse response = companyService.getCompanyProfile(ticker);
        return ResponseEntity.ok(response);
    }
}
