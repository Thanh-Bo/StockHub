package com.stockhub.company.service;

import com.stockhub.common.dto.PagedResponse;
import com.stockhub.common.exception.CompanyNotFoundException;
import com.stockhub.company.dto.CompanyResponse;
import com.stockhub.company.dto.CompanySummaryResponse;
import com.stockhub.company.dto.DashboardResponse;
import com.stockhub.company.dto.IndustryContext;
import com.stockhub.company.dto.PricePoint;
import com.stockhub.company.entity.Company;
import com.stockhub.company.entity.Industry;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.company.repository.IndustryRepository;
import com.stockhub.metrics.entity.FinancialRatio;
import com.stockhub.metrics.repository.FinancialRatioRepository;
import com.stockhub.prices.entity.StockPrice;
import com.stockhub.prices.repository.StockPriceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private static final int DASHBOARD_PRICE_HISTORY_DAYS = 30;

    private final CompanyRepository companyRepository;
    private final StockPriceRepository stockPriceRepository;
    private final FinancialRatioRepository financialRatioRepository;
    private final IndustryRepository industryRepository;

    public CompanyService(CompanyRepository companyRepository,
                          StockPriceRepository stockPriceRepository,
                          FinancialRatioRepository financialRatioRepository,
                          IndustryRepository industryRepository) {
        this.companyRepository = companyRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.financialRatioRepository = financialRatioRepository;
        this.industryRepository = industryRepository;
    }

    /**
     * Get detailed company profile by ticker.
     */
    public CompanyResponse getCompanyProfile(String ticker) {
        Company company = getCompanyByTicker(ticker);
        return mapToCompanyResponse(company);
    }

    /**
     * Get the raw Company entity by ticker for internal use.
     */
    public Company getCompanyByTicker(String ticker) {
        return companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));
    }

    /**
     * List active companies with pagination, returning summary DTOs.
     */
    public PagedResponse<CompanySummaryResponse> listCompanies(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Company> companyPage = companyRepository.findAll(pageable);

        List<CompanySummaryResponse> content = companyPage.getContent().stream()
                .map(this::mapToCompanySummaryResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                companyPage.getNumber(),
                companyPage.getSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages(),
                companyPage.isFirst(),
                companyPage.isLast()
        );
    }

    /**
     * Get the full dashboard for a ticker: profile + price + ratios + industry context.
     */
    public DashboardResponse getDashboard(String ticker) {
        Company company = getCompanyByTicker(ticker);
        UUID companyId = company.getId();

        StockPrice latestPrice = stockPriceRepository.findLatestByCompanyId(companyId).orElse(null);
        FinancialRatio latestRatio = getLatestAnnualRatio(companyId);
        Industry industry = company.getIndustryId() != null
                ? industryRepository.findById(company.getIndustryId()).orElse(null)
                : null;
        List<PricePoint> priceHistory = getPriceHistory(companyId, DASHBOARD_PRICE_HISTORY_DAYS);

        BigDecimal currentPrice = latestPrice != null ? latestPrice.getClose() : BigDecimal.ZERO;
        BigDecimal previousClose = getPreviousClose(companyId, latestPrice);
        BigDecimal priceChange = latestPrice != null
                ? currentPrice.subtract(previousClose)
                : BigDecimal.ZERO;
        BigDecimal priceChangePercent = previousClose.compareTo(BigDecimal.ZERO) != 0
                ? priceChange.divide(previousClose, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal dayHigh = latestPrice != null ? latestPrice.getHigh() : BigDecimal.ZERO;
        BigDecimal dayLow = latestPrice != null ? latestPrice.getLow() : BigDecimal.ZERO;
        Long volume = latestPrice != null ? latestPrice.getVolume() : 0L;

        IndustryContext industryContext = buildIndustryContext(industry, latestRatio);

        return new DashboardResponse(
                company.getTicker(),
                company.getName(),
                company.getDescription(),
                company.getSector(),
                company.getIndustry(),
                company.getHeadquarters(),
                company.getMarketCap(),
                company.getEmployees() != null ? company.getEmployees().longValue() : null,
                currentPrice,
                priceChange,
                priceChangePercent,
                dayHigh,
                dayLow,
                previousClose,
                volume,
                priceHistory,
                latestRatio != null ? latestRatio.getRevenueGrowthYoY() : null,
                latestRatio != null ? latestRatio.getEpsGrowthYoY() : null,
                latestRatio != null ? latestRatio.getRoe() : null,
                latestRatio != null ? latestRatio.getRoa() : null,
                latestRatio != null ? latestRatio.getPeRatio() : null,
                latestRatio != null ? latestRatio.getGrossMargin() : null,
                latestRatio != null ? latestRatio.getNetMargin() : null,
                latestRatio != null ? latestRatio.getDebtToEquity() : null,
                latestRatio != null ? latestRatio.getDividendYield() : null,
                industryContext,
                Instant.now(),
                "StockHub Database"
        );
    }

    // --- Private helper methods ---

    private CompanyResponse mapToCompanyResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getTicker(),
                company.getName(),
                company.getDescription(),
                company.getSector(),
                company.getIndustry(),
                company.getHeadquarters(),
                company.getMarketCap(),
                company.getEmployees() != null ? company.getEmployees().longValue() : null,
                company.getFoundedYear(),
                company.getWebsite(),
                company.getLogoUrl(),
                company.isActive()
        );
    }

    private CompanySummaryResponse mapToCompanySummaryResponse(Company company) {
        return new CompanySummaryResponse(
                company.getId(),
                company.getTicker(),
                company.getName(),
                company.getSector(),
                company.getMarketCap()
        );
    }

    private FinancialRatio getLatestAnnualRatio(UUID companyId) {
        return financialRatioRepository.findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                companyId,
                com.stockhub.common.enums.PeriodType.ANNUAL,
                PageRequest.of(0, 1)
        ).stream().findFirst().orElse(null);
    }

    private List<PricePoint> getPriceHistory(UUID companyId, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);
        List<StockPrice> prices = stockPriceRepository.findByCompanyIdAndDateBetweenOrderByDateDesc(
                companyId, from, to, Pageable.unpaged()
        );
        return prices.stream()
                .map(sp -> new PricePoint(sp.getDate(), sp.getClose(), sp.getAdjustedClose(), sp.getVolume()))
                .collect(Collectors.toList());
    }

    private BigDecimal getPreviousClose(UUID companyId, StockPrice latestPrice) {
        if (latestPrice == null) {
            return BigDecimal.ZERO;
        }
        List<StockPrice> prices = stockPriceRepository.findByCompanyIdAndDateBetweenOrderByDateDesc(
                companyId, latestPrice.getDate().minusDays(5), latestPrice.getDate().minusDays(1),
                PageRequest.of(0, 1)
        );
        return prices.isEmpty() ? latestPrice.getOpen() : prices.get(0).getClose();
    }

    private IndustryContext buildIndustryContext(Industry industry, FinancialRatio latestRatio) {
        if (industry == null) {
            return new IndustryContext(null, null, null, null, null, null, null, null);
        }

        BigDecimal avgPE = industry.getAvgPeRatio();
        BigDecimal avgRevenueGrowth = industry.getAvgRevenueGrowth();
        BigDecimal avgROE = null;
        BigDecimal avgNetMargin = null;
        BigDecimal pePercentile = null;
        BigDecimal roePercentile = null;

        if (latestRatio != null) {
            if (latestRatio.getPeRatio() != null && avgPE != null && avgPE.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = latestRatio.getPeRatio().divide(avgPE, 4, RoundingMode.HALF_UP);
                pePercentile = ratio;
            }
            if (latestRatio.getRoe() != null) {
                avgROE = latestRatio.getRoe();
            }
        }

        return new IndustryContext(
                industry.getSector(),
                industry.getIndustry(),
                avgPE,
                avgROE,
                avgRevenueGrowth,
                avgNetMargin,
                pePercentile,
                roePercentile
        );
    }
}
