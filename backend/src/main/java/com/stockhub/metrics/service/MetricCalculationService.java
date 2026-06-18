package com.stockhub.metrics.service;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.financials.entity.BalanceSheet;
import com.stockhub.financials.entity.CashFlowStatement;
import com.stockhub.financials.entity.IncomeStatement;
import com.stockhub.financials.repository.BalanceSheetRepository;
import com.stockhub.financials.repository.CashFlowStatementRepository;
import com.stockhub.financials.repository.IncomeStatementRepository;
import com.stockhub.metrics.entity.FinancialRatio;
import com.stockhub.metrics.repository.FinancialRatioRepository;
import com.stockhub.prices.entity.StockPrice;
import com.stockhub.prices.repository.StockPriceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MetricCalculationService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private final IncomeStatementRepository incomeStatementRepository;
    private final BalanceSheetRepository balanceSheetRepository;
    private final CashFlowStatementRepository cashFlowStatementRepository;
    private final StockPriceRepository stockPriceRepository;
    private final FinancialRatioRepository financialRatioRepository;
    private final CompanyRepository companyRepository;

    public MetricCalculationService(IncomeStatementRepository incomeStatementRepository,
                                    BalanceSheetRepository balanceSheetRepository,
                                    CashFlowStatementRepository cashFlowStatementRepository,
                                    StockPriceRepository stockPriceRepository,
                                    FinancialRatioRepository financialRatioRepository,
                                    CompanyRepository companyRepository) {
        this.incomeStatementRepository = incomeStatementRepository;
        this.balanceSheetRepository = balanceSheetRepository;
        this.cashFlowStatementRepository = cashFlowStatementRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.financialRatioRepository = financialRatioRepository;
        this.companyRepository = companyRepository;
    }

    // --- Growth Formulas ---

    /**
     * (current - previous) / |previous| * 100. Returns null if previous is null or zero.
     */
    public BigDecimal calculateRevenueGrowthYoY(IncomeStatement current, IncomeStatement previous) {
        return calculateGrowth(current != null ? current.getTotalRevenue() : null,
                               previous != null ? previous.getTotalRevenue() : null);
    }

    /**
     * EPS growth YoY: (current.eps - previous.eps) / |previous.eps| * 100
     */
    public BigDecimal calculateEpsGrowthYoY(IncomeStatement current, IncomeStatement previous) {
        return calculateGrowth(current != null ? current.getEps() : null,
                               previous != null ? previous.getEps() : null);
    }

    /**
     * FCF growth YoY: (current.freeCashFlow - previous.freeCashFlow) / |previous.freeCashFlow| * 100
     */
    public BigDecimal calculateFcfGrowthYoY(CashFlowStatement current, CashFlowStatement previous) {
        return calculateGrowth(current != null ? current.getFreeCashFlow() : null,
                               previous != null ? previous.getFreeCashFlow() : null);
    }

    /**
     * CAGR: (latest / historical)^(1/years) - 1, times 100. Returns null if not enough data.
     */
    public BigDecimal calculateRevenueGrowthCAGR(List<IncomeStatement> annual, int years) {
        if (annual == null || annual.size() < years + 1) {
            return null;
        }
        BigDecimal latest = annual.get(0).getTotalRevenue();
        BigDecimal historical = annual.get(years).getTotalRevenue();
        if (historical == null || historical.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (latest == null) {
            return null;
        }
        double ratio = latest.divide(historical, MC).doubleValue();
        double cagr = Math.pow(ratio, 1.0 / years) - 1.0;
        return BigDecimal.valueOf(cagr * 100).setScale(4, RoundingMode.HALF_UP);
    }

    // --- Profitability Formulas ---

    /**
     * ROE = netIncome / avgEquity * 100
     * avgEquity = (currentBS.equity + prevBS.equity) / 2
     */
    public BigDecimal calculateROE(IncomeStatement income, BalanceSheet currentBS, BalanceSheet prevBS) {
        if (income == null || income.getNetIncome() == null) return null;
        BigDecimal equity = currentBS != null ? currentBS.getTotalShareholderEquity() : null;
        BigDecimal prevEquity = prevBS != null ? prevBS.getTotalShareholderEquity() : null;
        if (equity == null) return null;
        BigDecimal avgEquity = prevEquity != null
                ? equity.add(prevEquity).divide(BigDecimal.valueOf(2), MC)
                : equity;
        if (avgEquity.compareTo(BigDecimal.ZERO) == 0) return null;
        return income.getNetIncome()
                .divide(avgEquity, MC)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * ROA = netIncome / avgAssets * 100
     */
    public BigDecimal calculateROA(IncomeStatement income, BalanceSheet currentBS, BalanceSheet prevBS) {
        if (income == null || income.getNetIncome() == null) return null;
        BigDecimal assets = currentBS != null ? currentBS.getTotalAssets() : null;
        BigDecimal prevAssets = prevBS != null ? prevBS.getTotalAssets() : null;
        if (assets == null) return null;
        BigDecimal avgAssets = prevAssets != null
                ? assets.add(prevAssets).divide(BigDecimal.valueOf(2), MC)
                : assets;
        if (avgAssets.compareTo(BigDecimal.ZERO) == 0) return null;
        return income.getNetIncome()
                .divide(avgAssets, MC)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Margin = numerator / denominator * 100. Returns null if denominator is null or zero.
     */
    public BigDecimal calculateMargin(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.divide(denominator, MC)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    // --- Valuation Formulas ---

    /**
     * PE = stockPrice / eps. Returns null if eps <= 0.
     */
    public BigDecimal calculatePE(BigDecimal stockPrice, BigDecimal eps) {
        if (stockPrice == null || eps == null || eps.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return stockPrice.divide(eps, 4, RoundingMode.HALF_UP);
    }

    /**
     * PEG = pe / epsGrowth. Returns null if growth is null or <= 0.
     */
    public BigDecimal calculatePEG(BigDecimal pe, BigDecimal epsGrowth) {
        if (pe == null || epsGrowth == null || epsGrowth.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return pe.divide(epsGrowth, 4, RoundingMode.HALF_UP);
    }

    /**
     * Price to Book = price / (equity / sharesOutstanding).
     */
    public BigDecimal calculatePriceToBook(BigDecimal price, BalanceSheet bs) {
        if (price == null || bs == null || bs.getTotalShareholderEquity() == null
                || bs.getSharesOutstanding() == null || bs.getSharesOutstanding() == 0
                || bs.getTotalShareholderEquity().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal bookPerShare = bs.getTotalShareholderEquity()
                .divide(BigDecimal.valueOf(bs.getSharesOutstanding()), MC);
        return price.divide(bookPerShare, 4, RoundingMode.HALF_UP);
    }

    /**
     * Dividend Yield = sum of last 4 quarterly dividends / price * 100
     */
    public BigDecimal calculateDividendYield(BigDecimal price, UUID companyId) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        List<CashFlowStatement> quarterlyCF = cashFlowStatementRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        companyId, PeriodType.QUARTERLY, PageRequest.of(0, 4));
        BigDecimal totalDividends = BigDecimal.ZERO;
        for (CashFlowStatement cf : quarterlyCF) {
            if (cf.getDividendsPaid() != null) {
                totalDividends = totalDividends.add(cf.getDividendsPaid());
            }
        }
        if (totalDividends.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return totalDividends.divide(price, MC)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    // --- Financial Health Formulas ---

    /**
     * Debt to Equity = totalDebt / equity. Returns null if equity is null or zero.
     */
    public BigDecimal calculateDebtToEquity(BalanceSheet bs) {
        if (bs == null || bs.getTotalDebt() == null || bs.getTotalShareholderEquity() == null
                || bs.getTotalShareholderEquity().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return bs.getTotalDebt().divide(bs.getTotalShareholderEquity(), 4, RoundingMode.HALF_UP);
    }

    /**
     * Current Ratio = currentAssets / currentLiabilities.
     */
    public BigDecimal calculateCurrentRatio(BalanceSheet bs) {
        if (bs == null || bs.getTotalCurrentAssets() == null || bs.getTotalCurrentLiabilities() == null
                || bs.getTotalCurrentLiabilities().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return bs.getTotalCurrentAssets()
                .divide(bs.getTotalCurrentLiabilities(), 4, RoundingMode.HALF_UP);
    }

    // --- Aggregate Calculation ---

    /**
     * Calculate all metrics for a ticker at a given date. Returns a filled FinancialRatio entity (not persisted).
     */
    public FinancialRatio calculateMetrics(String ticker, LocalDate asOfDate) {
        Company company = companyRepository.findByTicker(ticker.toUpperCase())
                .orElse(null);
        if (company == null) {
            return null;
        }
        UUID companyId = company.getId();

        // Fetch latest annual income statement at or before asOfDate
        List<IncomeStatement> annualIncome = incomeStatementRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        companyId, PeriodType.ANNUAL, PageRequest.of(0, 10));
        IncomeStatement currentIS = annualIncome.isEmpty() ? null : annualIncome.get(0);
        IncomeStatement prevIS = annualIncome.size() > 1 ? annualIncome.get(1) : null;

        // Fetch latest annual balance sheet
        List<BalanceSheet> annualBS = balanceSheetRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        companyId, PeriodType.ANNUAL, PageRequest.of(0, 2));
        BalanceSheet currentBS = annualBS.isEmpty() ? null : annualBS.get(0);
        BalanceSheet prevBS = annualBS.size() > 1 ? annualBS.get(1) : null;

        // Fetch latest annual cash flow
        List<CashFlowStatement> annualCF = cashFlowStatementRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        companyId, PeriodType.ANNUAL, PageRequest.of(0, 2));
        CashFlowStatement currentCF = annualCF.isEmpty() ? null : annualCF.get(0);
        CashFlowStatement prevCF = annualCF.size() > 1 ? annualCF.get(1) : null;

        // Latest stock price
        StockPrice latestPrice = stockPriceRepository.findLatestByCompanyId(companyId).orElse(null);
        BigDecimal price = latestPrice != null ? latestPrice.getClose() : null;

        // Compute all metrics
        BigDecimal revenueGrowthYoY = calculateRevenueGrowthYoY(currentIS, prevIS);
        BigDecimal revenueGrowth3y = calculateRevenueGrowthCAGR(annualIncome, 3);
        BigDecimal revenueGrowth5y = calculateRevenueGrowthCAGR(annualIncome, 5);
        BigDecimal epsGrowthYoY = calculateEpsGrowthYoY(currentIS, prevIS);
        BigDecimal fcfGrowthYoY = calculateFcfGrowthYoY(currentCF, prevCF);
        BigDecimal roe = calculateROE(currentIS, currentBS, prevBS);
        BigDecimal roa = calculateROA(currentIS, currentBS, prevBS);
        BigDecimal debtToEquity = calculateDebtToEquity(currentBS);
        BigDecimal currentRatio = calculateCurrentRatio(currentBS);

        BigDecimal grossProfit = currentIS != null ? currentIS.getGrossProfit() : null;
        BigDecimal totalRevenue = currentIS != null ? currentIS.getTotalRevenue() : null;
        BigDecimal operatingIncome = currentIS != null ? currentIS.getOperatingIncome() : null;
        BigDecimal netIncome = currentIS != null ? currentIS.getNetIncome() : null;
        BigDecimal grossMargin = calculateMargin(grossProfit, totalRevenue);
        BigDecimal operatingMargin = calculateMargin(operatingIncome, totalRevenue);
        BigDecimal netMargin = calculateMargin(netIncome, totalRevenue);

        BigDecimal eps = currentIS != null ? currentIS.getEps() : null;
        BigDecimal peRatio = calculatePE(price, eps);
        BigDecimal pegRatio = calculatePEG(peRatio, epsGrowthYoY);
        BigDecimal dividendYield = calculateDividendYield(price, companyId);
        BigDecimal priceToBook = calculatePriceToBook(price, currentBS);

        LocalDate fiscalDateEnding = currentIS != null
                ? currentIS.getFiscalDateEnding()
                : asOfDate;

        return FinancialRatio.builder()
                .companyId(companyId)
                .fiscalDateEnding(fiscalDateEnding)
                .periodType(PeriodType.ANNUAL)
                .revenueGrowthYoY(revenueGrowthYoY)
                .revenueGrowth3y(revenueGrowth3y)
                .revenueGrowth5y(revenueGrowth5y)
                .epsGrowthYoY(epsGrowthYoY)
                .fcfGrowthYoY(fcfGrowthYoY)
                .roe(roe)
                .roa(roa)
                .debtToEquity(debtToEquity)
                .grossMargin(grossMargin)
                .operatingMargin(operatingMargin)
                .netMargin(netMargin)
                .peRatio(peRatio)
                .pegRatio(pegRatio)
                .dividendYield(dividendYield)
                .priceToBook(priceToBook)
                .currentRatio(currentRatio)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Calculate and persist metrics for multiple tickers. Uses ON CONFLICT resolution.
     */
    @Transactional
    public void calculateAndPersistMetrics(List<String> tickers) {
        LocalDate today = LocalDate.now();
        for (String ticker : tickers) {
            FinancialRatio ratio = calculateMetrics(ticker, today);
            if (ratio != null) {
                // Check if a ratio already exists for this company/period/fiscalDate
                Optional<FinancialRatio> existing = financialRatioRepository
                        .findByCompanyIdAndPeriodTypeAndFiscalDateEnding(
                                ratio.getCompanyId(), ratio.getPeriodType(), ratio.getFiscalDateEnding());
                if (existing.isPresent()) {
                    FinancialRatio existingRatio = existing.get();
                    existingRatio.setRevenueGrowthYoY(ratio.getRevenueGrowthYoY());
                    existingRatio.setRevenueGrowth3y(ratio.getRevenueGrowth3y());
                    existingRatio.setRevenueGrowth5y(ratio.getRevenueGrowth5y());
                    existingRatio.setEpsGrowthYoY(ratio.getEpsGrowthYoY());
                    existingRatio.setFcfGrowthYoY(ratio.getFcfGrowthYoY());
                    existingRatio.setRoe(ratio.getRoe());
                    existingRatio.setRoa(ratio.getRoa());
                    existingRatio.setDebtToEquity(ratio.getDebtToEquity());
                    existingRatio.setGrossMargin(ratio.getGrossMargin());
                    existingRatio.setOperatingMargin(ratio.getOperatingMargin());
                    existingRatio.setNetMargin(ratio.getNetMargin());
                    existingRatio.setPeRatio(ratio.getPeRatio());
                    existingRatio.setPegRatio(ratio.getPegRatio());
                    existingRatio.setDividendYield(ratio.getDividendYield());
                    existingRatio.setPriceToBook(ratio.getPriceToBook());
                    existingRatio.setCurrentRatio(ratio.getCurrentRatio());
                    financialRatioRepository.save(existingRatio);
                } else {
                    financialRatioRepository.save(ratio);
                }
            }
        }
    }

    /**
     * TTM EPS = Sum of last 4 quarterly EPS.
     */
    public BigDecimal getTTMEPS(UUID companyId) {
        List<IncomeStatement> quarterly = incomeStatementRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        companyId, PeriodType.QUARTERLY, PageRequest.of(0, 4));
        BigDecimal sum = BigDecimal.ZERO;
        for (IncomeStatement is : quarterly) {
            if (is.getEps() != null) {
                sum = sum.add(is.getEps());
            }
        }
        return sum.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * TTM Revenue = Sum of last 4 quarterly revenues.
     */
    public BigDecimal getTTMRevenue(UUID companyId) {
        List<IncomeStatement> quarterly = incomeStatementRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        companyId, PeriodType.QUARTERLY, PageRequest.of(0, 4));
        BigDecimal sum = BigDecimal.ZERO;
        for (IncomeStatement is : quarterly) {
            if (is.getTotalRevenue() != null) {
                sum = sum.add(is.getTotalRevenue());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    // --- Private helpers ---

    private BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous.abs(), MC)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
