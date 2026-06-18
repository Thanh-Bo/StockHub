package com.stockhub.comparison.service;

import com.stockhub.comparison.dto.CompanyComparisonRow;
import com.stockhub.comparison.dto.ComparisonRequest;
import com.stockhub.comparison.dto.ComparisonResponse;
import com.stockhub.comparison.dto.IndustryAveragesResponse;
import com.stockhub.common.enums.PeriodType;
import com.stockhub.common.exception.CompanyNotFoundException;
import com.stockhub.common.exception.InvalidComparisonException;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.metrics.entity.FinancialRatio;
import com.stockhub.metrics.repository.FinancialRatioRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ComparisonService {

    private static final String CACHE_PREFIX = "comparison::";
    private static final long CACHE_TTL_HOURS = 1;

    private final CompanyRepository companyRepository;
    private final FinancialRatioRepository financialRatioRepository;
    private final IndustryService industryService;
    private final RedisTemplate<String, Object> redisTemplate;

    public ComparisonService(CompanyRepository companyRepository,
                             FinancialRatioRepository financialRatioRepository,
                             IndustryService industryService,
                             RedisTemplate<String, Object> redisTemplate) {
        this.companyRepository = companyRepository;
        this.financialRatioRepository = financialRatioRepository;
        this.industryService = industryService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Compare multiple companies side by side (2-5 tickers).
     */
    public ComparisonResponse compare(ComparisonRequest request) {
        List<String> tickers = request.tickers().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        if (tickers.size() < 2 || tickers.size() > 5) {
            throw new InvalidComparisonException("Must compare between 2 and 5 tickers");
        }

        // Check cache
        String cacheKey = CACHE_PREFIX + String.join(",", tickers);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ComparisonResponse cr) {
            return cr;
        }

        // Fetch companies
        List<Company> companies = companyRepository.findByTickerIn(tickers);
        if (companies.size() < 2) {
            throw new InvalidComparisonException("Could not find all requested tickers");
        }

        // Fetch latest annual FinancialRatio for each
        List<CompanyComparisonRow> rows = new ArrayList<>();
        String primarySector = null;
        String primaryIndustry = null;

        for (Company company : companies) {
            List<FinancialRatio> ratios = financialRatioRepository
                    .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                            company.getId(), PeriodType.ANNUAL,
                            org.springframework.data.domain.PageRequest.of(0, 1));

            FinancialRatio ratio = ratios.isEmpty() ? null : ratios.get(0);

            Map<String, BigDecimal> metrics = new LinkedHashMap<>();
            if (ratio != null) {
                metrics.put("marketCap", company.getMarketCap());
                metrics.put("revenueGrowthYoY", ratio.getRevenueGrowthYoY());
                metrics.put("epsGrowthYoY", ratio.getEpsGrowthYoY());
                metrics.put("fcfGrowthYoY", ratio.getFcfGrowthYoY());
                metrics.put("roe", ratio.getRoe());
                metrics.put("roa", ratio.getRoa());
                metrics.put("debtToEquity", ratio.getDebtToEquity());
                metrics.put("grossMargin", ratio.getGrossMargin());
                metrics.put("operatingMargin", ratio.getOperatingMargin());
                metrics.put("netMargin", ratio.getNetMargin());
                metrics.put("peRatio", ratio.getPeRatio());
                metrics.put("pegRatio", ratio.getPegRatio());
                metrics.put("dividendYield", ratio.getDividendYield());
                metrics.put("priceToBook", ratio.getPriceToBook());
                metrics.put("currentRatio", ratio.getCurrentRatio());
            }

            rows.add(new CompanyComparisonRow(
                    company.getTicker(),
                    company.getName(),
                    company.getSector(),
                    company.getIndustry(),
                    metrics
            ));

            if (primarySector == null && company.getSector() != null) {
                primarySector = company.getSector();
                primaryIndustry = company.getIndustry();
            }
        }

        // Fetch industry averages if requested
        IndustryAveragesResponse industryAverages = null;
        if (request.includeIndustryAverages() && primarySector != null && primaryIndustry != null) {
            industryAverages = industryService.getAverages(primarySector, primaryIndustry);
        }

        ComparisonResponse response = new ComparisonResponse(rows, industryAverages);

        // Cache result
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);

        return response;
    }

    private Company getCompany(String ticker) {
        return companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));
    }
}
