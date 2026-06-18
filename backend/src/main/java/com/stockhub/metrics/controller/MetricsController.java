package com.stockhub.metrics.controller;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.common.exception.CompanyNotFoundException;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.metrics.dto.MetricsResponse;
import com.stockhub.metrics.entity.FinancialRatio;
import com.stockhub.metrics.repository.FinancialRatioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/companies/{ticker}/metrics")
public class MetricsController {

    private final FinancialRatioRepository financialRatioRepository;
    private final CompanyRepository companyRepository;

    public MetricsController(FinancialRatioRepository financialRatioRepository,
                             CompanyRepository companyRepository) {
        this.financialRatioRepository = financialRatioRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * GET / → get latest FinancialRatio for ticker.
     */
    @GetMapping
    public ResponseEntity<MetricsResponse> getLatestMetrics(@PathVariable String ticker) {
        Company company = getCompany(ticker);
        List<FinancialRatio> ratios = financialRatioRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        company.getId(), PeriodType.ANNUAL, PageRequest.of(0, 1));

        if (ratios.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        FinancialRatio ratio = ratios.get(0);
        return ResponseEntity.ok(toMetricsResponse(company.getTicker(), company.getName(), ratio));
    }

    /**
     * GET /history?period=ANNUAL&years=5 → list historical ratios.
     */
    @GetMapping("/history")
    public ResponseEntity<List<MetricsResponse>> getMetricsHistory(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "ANNUAL") String period,
            @RequestParam(defaultValue = "5") int years) {
        Company company = getCompany(ticker);
        PeriodType periodType;
        try {
            periodType = PeriodType.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            periodType = PeriodType.ANNUAL;
        }

        int pageSize = years > 0 ? years : 5;
        List<FinancialRatio> ratios = financialRatioRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        company.getId(), periodType, PageRequest.of(0, pageSize));

        List<MetricsResponse> response = ratios.stream()
                .map(r -> toMetricsResponse(company.getTicker(), company.getName(), r))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private Company getCompany(String ticker) {
        return companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));
    }

    private MetricsResponse toMetricsResponse(String ticker, String name, FinancialRatio r) {
        return new MetricsResponse(
                ticker,
                name,
                r.getFiscalDateEnding(),
                r.getRevenueGrowthYoY(),
                r.getRevenueGrowth3y(),
                r.getRevenueGrowth5y(),
                r.getEpsGrowthYoY(),
                r.getFcfGrowthYoY(),
                r.getRoe(),
                r.getRoa(),
                r.getDebtToEquity(),
                r.getGrossMargin(),
                r.getOperatingMargin(),
                r.getNetMargin(),
                r.getPeRatio(),
                r.getPegRatio(),
                r.getDividendYield(),
                r.getPriceToBook(),
                r.getCurrentRatio()
        );
    }
}
