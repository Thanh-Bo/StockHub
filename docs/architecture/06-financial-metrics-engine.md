# Section 6: Financial Metrics Engine

## 6.1 Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                  MetricCalculationService                      │
│                                                               │
│  Input: Company ticker (or list of tickers during ETL)        │
│  Data: IncomeStatement, BalanceSheet, CashFlowStatement,      │
│        StockPrice (latest close)                               │
│  Output: FinancialRatio entity → financial_ratios table        │
│                                                               │
│  Design Decision: All metrics calculated in Java, not SQL.    │
│  Rationale:                                                   │
│    • Testable with unit tests                                 │
│    • Easier to debug (step-through)                           │
│    • Type-safe BigDecimal arithmetic                          │
│    • Postgres does the data retrieval, Java does the math     │
└───────────────────────────────────────────────────────────────┘
```

---

## 6.2 Metric Formulas & Implementation

### 6.2.1 Growth Metrics

#### Revenue Growth (YoY)
```
RevenueGrowth_YoY = (Revenue_T - Revenue_T-1) / |Revenue_T-1| × 100
```

```java
public BigDecimal calculateRevenueGrowthYoY(IncomeStatement current, IncomeStatement previous) {
    if (previous == null || previous.getTotalRevenue().compareTo(BigDecimal.ZERO) == 0) {
        return null;
    }
    return current.getTotalRevenue()
        .subtract(previous.getTotalRevenue())
        .divide(previous.getTotalRevenue().abs(), 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
}
```

#### Revenue Growth (3-Year CAGR)
```
RevenueGrowth_3Y = [(Revenue_T / Revenue_T-3) ^ (1/3) - 1] × 100
```

```java
public BigDecimal calculateRevenueGrowthCAGR(List<IncomeStatement> annualStatements, int years) {
    if (annualStatements.size() < years + 1) return null;

    IncomeStatement latest = annualStatements.get(0);
    IncomeStatement historical = annualStatements.get(years);

    BigDecimal ratio = latest.getTotalRevenue()
        .divide(historical.getTotalRevenue(), 10, RoundingMode.HALF_UP);

    // CAGR = ratio^(1/years) - 1
    double cagr = Math.pow(ratio.doubleValue(), 1.0 / years) - 1.0;
    return BigDecimal.valueOf(cagr * 100).setScale(4, RoundingMode.HALF_UP);
}
```

#### EPS Growth (YoY)
```
EPSGrowth_YoY = (EPS_T - EPS_T-1) / |EPS_T-1| × 100
```

#### Free Cash Flow Growth (YoY)
```
FCFGrowth_YoY = (FCF_T - FCF_T-1) / |FCF_T-1| × 100
```

Where: `FCF = OperatingCashFlow - CapitalExpenditure`

---

### 6.2.2 Profitability Metrics

#### Return on Equity (ROE)
```
ROE = NetIncome_T / AverageShareholderEquity × 100

AverageShareholderEquity = (Equity_T + Equity_T-1) / 2
```

```java
public BigDecimal calculateROE(IncomeStatement income, BalanceSheet currentBS, BalanceSheet previousBS) {
    if (currentBS.getTotalShareholderEquity() == null
            || currentBS.getTotalShareholderEquity().compareTo(BigDecimal.ZERO) == 0) {
        return null;
    }
    BigDecimal avgEquity = currentBS.getTotalShareholderEquity()
        .add(previousBS != null ? previousBS.getTotalShareholderEquity()
              : currentBS.getTotalShareholderEquity())
        .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);

    return income.getNetIncome()
        .divide(avgEquity, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100));
}
```

#### Return on Assets (ROA)
```
ROA = NetIncome_T / AverageTotalAssets × 100
```

#### Gross Margin
```
GrossMargin = GrossProfit / TotalRevenue × 100
```

#### Operating Margin
```
OperatingMargin = OperatingIncome / TotalRevenue × 100
```

#### Net Margin
```
NetMargin = NetIncome / TotalRevenue × 100
```

---

### 6.2.3 Valuation Metrics

#### P/E Ratio
```
PE_Ratio = StockPrice / EPS

Uses: Latest adjusted close price and trailing-twelve-month (TTM) EPS
```

```java
public BigDecimal calculatePE(BigDecimal stockPrice, BigDecimal ttmEPS) {
    if (ttmEPS == null || ttmEPS.compareTo(BigDecimal.ZERO) <= 0) {
        return null; // P/E undefined for negative earnings
    }
    return stockPrice.divide(ttmEPS, 4, RoundingMode.HALF_UP);
}
```

#### PEG Ratio
```
PEG_Ratio = PE_Ratio / EPSGrowth_YoY

Only meaningful when EPSGrowth_YoY > 0
```

#### Price to Book
```
PriceToBook = StockPrice / (ShareholderEquity / SharesOutstanding)
```

#### Dividend Yield
```
DividendYield = (AnnualDividendsPerShare / StockPrice) × 100

AnnualDividendsPerShare = Sum of last 4 quarterly dividends from CashFlowStatement
```

---

### 6.2.4 Leverage & Liquidity Metrics

#### Debt to Equity
```
DebtToEquity = TotalDebt / ShareholderEquity
```

#### Current Ratio
```
CurrentRatio = CurrentAssets / CurrentLiabilities
```

---

## 6.3 Service Implementation

```java
package com.stockhub.metrics;

@Service
@Transactional(readOnly = true)
public class MetricCalculationService {

    private final IncomeStatementRepository incomeRepo;
    private final BalanceSheetRepository balanceRepo;
    private final CashFlowStatementRepository cashFlowRepo;
    private final FinancialRatioRepository ratioRepo;
    private final PriceRepository priceRepo;

    /**
     * Calculate all metrics for a single company.
     * Called during ETL and on-demand when viewing dashboard.
     */
    public FinancialRatio calculateMetrics(String ticker, LocalDate asOfDate) {
        Company company = companyRepo.findByTicker(ticker)
            .orElseThrow(() -> new CompanyNotFoundException(ticker));

        // Fetch required data
        List<IncomeStatement> annualIncome = incomeRepo
            .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                company.getId(), PeriodType.ANNUAL, Pageable.ofSize(11));
        List<BalanceSheet> annualBalance = balanceRepo
            .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                company.getId(), PeriodType.ANNUAL, Pageable.ofSize(11));
        List<CashFlowStatement> annualCashFlow = cashFlowRepo
            .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                company.getId(), PeriodType.ANNUAL, Pageable.ofSize(11));
        BigDecimal stockPrice = priceRepo.findLatestClose(company.getId())
            .orElse(BigDecimal.ZERO);

        // Build ratio entity
        FinancialRatio ratio = new FinancialRatio();
        ratio.setCompanyId(company.getId());
        ratio.setFiscalDateEnding(asOfDate);
        ratio.setPeriodType(PeriodType.ANNUAL);

        if (annualIncome.size() >= 2) {
            ratio.setRevenueGrowthYoY(calculateRevenueGrowthYoY(
                annualIncome.get(0), annualIncome.get(1)));
            ratio.setEpsGrowthYoY(calculateEpsGrowthYoY(
                annualIncome.get(0), annualIncome.get(1)));
        }
        if (annualIncome.size() >= 4) {
            ratio.setRevenueGrowth3Y(calculateCAGR(
                annualIncome, 3, IncomeStatement::getTotalRevenue));
        }
        if (annualIncome.size() >= 6) {
            ratio.setRevenueGrowth5Y(calculateCAGR(
                annualIncome, 5, IncomeStatement::getTotalRevenue));
        }
        if (annualCashFlow.size() >= 2) {
            ratio.setFcfGrowthYoY(calculateFcfGrowthYoY(
                annualCashFlow.get(0), annualCashFlow.get(1)));
        }
        if (!annualIncome.isEmpty() && !annualBalance.isEmpty()) {
            ratio.setRoe(calculateROE(
                annualIncome.get(0), annualBalance.get(0),
                annualBalance.size() >= 2 ? annualBalance.get(1) : annualBalance.get(0)));
            ratio.setRoa(calculateROA(
                annualIncome.get(0), annualBalance.get(0),
                annualBalance.size() >= 2 ? annualBalance.get(1) : annualBalance.get(0)));
            ratio.setGrossMargin(calculateMargin(
                annualIncome.get(0).getGrossProfit(),
                annualIncome.get(0).getTotalRevenue()));
            ratio.setOperatingMargin(calculateMargin(
                annualIncome.get(0).getOperatingIncome(),
                annualIncome.get(0).getTotalRevenue()));
            ratio.setNetMargin(calculateMargin(
                annualIncome.get(0).getNetIncome(),
                annualIncome.get(0).getTotalRevenue()));
            ratio.setDebtToEquity(calculateDebtToEquity(annualBalance.get(0)));
            ratio.setCurrentRatio(calculateCurrentRatio(annualBalance.get(0)));
        }
        if (stockPrice.compareTo(BigDecimal.ZERO) > 0) {
            ratio.setPeRatio(calculatePE(stockPrice, getTTMEPS(company.getId())));
            ratio.setPegRatio(calculatePEG(ratio.getPeRatio(), ratio.getEpsGrowthYoY()));
            ratio.setPriceToBook(calculatePriceToBook(
                stockPrice, annualBalance.get(0)));
            ratio.setDividendYield(calculateDividendYield(
                stockPrice, company.getId()));
        }

        return ratio;
    }

    // Bulk calculation for ETL — processes all companies
    public void calculateAndPersistMetrics(List<String> tickers) {
        tickers.forEach(ticker -> {
            try {
                FinancialRatio ratio = calculateMetrics(ticker, LocalDate.now());
                ratioRepo.upsert(ratio); // ON CONFLICT UPDATE
            } catch (Exception e) {
                log.warn("Failed to calculate metrics for {}", ticker, e);
            }
        });
    }
}
```

---

## 6.4 TTM (Trailing Twelve Months) Calculations

For quarterly data (most recent 4 quarters summed):

```java
private BigDecimal getTTMRevenue(UUID companyId) {
    List<IncomeStatement> last4Q = incomeRepo
        .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
            companyId, PeriodType.QUARTERLY, Pageable.ofSize(4));

    return last4Q.stream()
        .map(IncomeStatement::getTotalRevenue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}

private BigDecimal getTTMEPS(UUID companyId) {
    List<IncomeStatement> last4Q = incomeRepo
        .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
            companyId, PeriodType.QUARTERLY, Pageable.ofSize(4));

    return last4Q.stream()
        .map(IncomeStatement::getEps)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

---

## 6.5 Edge Cases Handled

| Edge Case | Handling |
|-----------|----------|
| Negative earnings (P/E) | P/E = null, UI shows "N/A" |
| Negative equity (D/E) | D/E = null, flagged |
| Zero revenue (margins) | Margin = null |
| Missing historical data (growth) | Growth = null for unavailable periods |
| Bank/insurance companies | Debt-to-Equity is less meaningful; UI labels adjust |
| Restated financials | ETL uses latest filing (ON CONFLICT upsert overwrites) |
| Non-calendar fiscal years | Uses `fiscal_year` from SEC data, not calendar year |
| Stock splits | `adjusted_close` in stock_prices handles splits |

---

## 6.6 Testing Strategy for Metrics Engine

```java
@SpringBootTest
class MetricCalculationServiceTest {

    @Test
    void shouldCalculateRevenueGrowthCorrectly() {
        // Given: Revenue grew from $100M to $120M
        IncomeStatement current = createIncomeStatement(120_000_000);
        IncomeStatement previous = createIncomeStatement(100_000_000);

        // When
        BigDecimal growth = service.calculateRevenueGrowthYoY(current, previous);

        // Then: 20% growth
        assertThat(growth).isEqualByComparingTo(new BigDecimal("20.0000"));
    }

    @Test
    void shouldReturnNullWhenPreviousYearHasZeroRevenue() {
        IncomeStatement current = createIncomeStatement(120_000_000);
        IncomeStatement previous = createIncomeStatement(0);

        BigDecimal growth = service.calculateRevenueGrowthYoY(current, previous);

        assertThat(growth).isNull();
    }

    @Test
    void shouldHandleNegativeToPositiveGrowth() {
        // Revenue went from -$10M to $50M
        IncomeStatement current = createIncomeStatement(50_000_000);
        IncomeStatement previous = createIncomeStatement(-10_000_000);

        BigDecimal growth = service.calculateRevenueGrowthYoY(current, previous);

        // Growth = ($50M - (-$10M)) / |-10M| = $60M/10M = 600%
        assertThat(growth).isEqualByComparingTo(new BigDecimal("600.0000"));
    }
}
```
