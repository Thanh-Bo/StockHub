# Section 9: Industry Comparison Engine

## 9.1 Feature Overview

Users can compare 2-5 companies side-by-side across:
- **Growth**: Revenue growth, EPS growth, FCF growth
- **Margins**: Gross, operating, net margins
- **Valuation**: P/E, PEG, P/B, Dividend Yield
- **Profitability**: ROE, ROA
- **Financial Health**: Debt/Equity, Current Ratio
- **Price Performance**: 1Y, 5Y price change %

---

## 9.2 API Design

```
POST /api/v1/companies/compare
```

**Request:**
```json
{
  "tickers": ["AAPL", "MSFT", "GOOGL"],
  "metrics": [
    "revenueGrowthYoY", "epsGrowthYoY", "roe", "roa",
    "peRatio", "pegRatio", "grossMargin", "operatingMargin",
    "netMargin", "debtToEquity", "dividendYield", "marketCap"
  ],
  "includeIndustryAverages": true
}
```

**Response:**
```json
{
  "companies": [
    {
      "ticker": "AAPL",
      "name": "Apple Inc.",
      "sector": "Technology",
      "industry": "Consumer Electronics",
      "metrics": {
        "revenueGrowthYoY": 5.2,
        "epsGrowthYoY": 8.1,
        "roe": 145.3,
        "roa": 28.5,
        "peRatio": 28.5,
        "pegRatio": 3.5,
        "grossMargin": 44.1,
        "operatingMargin": 29.8,
        "netMargin": 26.4,
        "debtToEquity": 1.95,
        "dividendYield": 0.52,
        "marketCap": 2800000000000
      }
    }
  ],
  "industryAverages": {
    "sector": "Technology",
    "avgRevenueGrowthYoY": 12.3,
    "avgPE": 25.1,
    "avgROE": 35.2,
    "avgNetMargin": 18.7
  }
}
```

---

## 9.3 Backend Implementation

### Service Layer

```java
package com.stockhub.comparison;

@Service
@Transactional(readOnly = true)
public class ComparisonService {

    private final CompanyRepository companyRepo;
    private final FinancialRatioRepository ratioRepo;
    private final PriceRepository priceRepo;
    private final IndustryService industryService;
    private final RedisTemplate<String, ComparisonResponse> redisTemplate;

    @Cacheable(value = "comparison", key = "#request.tickers().hashCode()")
    public ComparisonResponse compare(ComparisonRequest request) {
        List<String> tickers = request.tickers();
        if (tickers.size() < 2 || tickers.size() > 5) {
            throw new InvalidComparisonException("Compare 2-5 companies");
        }

        // Fetch latest financial ratios for all companies
        List<Company> companies = companyRepo.findByTickerIn(tickers);
        Map<UUID, FinancialRatio> ratios = ratioRepo
            .findLatestAnnualRatios(companies.stream().map(Company::getId).toList());

        // Build comparison rows
        List<CompanyComparisonRow> rows = companies.stream()
            .map(company -> buildComparisonRow(company, ratios.get(company.getId())))
            .toList();

        // Fetch industry averages
        IndustryAverages industryAverages = null;
        if (request.includeIndustryAverages()) {
            String sector = companies.get(0).getSector();
            industryAverages = industryService.getAverages(sector);
        }

        return new ComparisonResponse(rows, industryAverages);
    }

    private CompanyComparisonRow buildComparisonRow(Company company, FinancialRatio ratio) {
        return CompanyComparisonRow.builder()
            .ticker(company.getTicker())
            .name(company.getName())
            .sector(company.getSector())
            .industry(company.getIndustry())
            .metrics(buildMetricsMap(ratio, company))
            .build();
    }

    private Map<String, BigDecimal> buildMetricsMap(FinancialRatio ratio, Company company) {
        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        metrics.put("marketCap", company.getMarketCap());
        metrics.put("revenueGrowthYoY", ratio.getRevenueGrowthYoY());
        metrics.put("epsGrowthYoY", ratio.getEpsGrowthYoY());
        metrics.put("fcfGrowthYoY", ratio.getFcfGrowthYoY());
        metrics.put("roe", ratio.getRoe());
        metrics.put("roa", ratio.getRoa());
        metrics.put("peRatio", ratio.getPeRatio());
        metrics.put("pegRatio", ratio.getPegRatio());
        metrics.put("grossMargin", ratio.getGrossMargin());
        metrics.put("operatingMargin", ratio.getOperatingMargin());
        metrics.put("netMargin", ratio.getNetMargin());
        metrics.put("debtToEquity", ratio.getDebtToEquity());
        metrics.put("dividendYield", ratio.getDividendYield());
        metrics.put("priceToBook", ratio.getPriceToBook());
        metrics.put("currentRatio", ratio.getCurrentRatio());
        return metrics;
    }
}
```

---

## 9.4 Industry Aggregation with Window Functions

PostgreSQL window functions compute industry percentiles for richer comparison context:

```sql
-- Get company metrics with industry percentile rankings
SELECT
    c.ticker,
    c.name,
    fr.pe_ratio,
    fr.roe,
    fr.revenue_growth_yoy,
    fr.net_margin,
    -- Percentile rank within the same industry
    PERCENT_RANK() OVER (
        PARTITION BY c.industry
        ORDER BY fr.roe DESC
    ) AS roe_percentile,
    PERCENT_RANK() OVER (
        PARTITION BY c.industry
        ORDER BY fr.revenue_growth_yoy DESC
    ) AS growth_percentile,
    PERCENT_RANK() OVER (
        PARTITION BY c.industry
        ORDER BY fr.pe_ratio ASC
    ) AS pe_percentile
FROM company c
JOIN financial_ratios fr ON fr.company_id = c.id
    AND fr.period_type = 'ANNUAL'
    AND fr.fiscal_date_ending = (
        SELECT MAX(fiscal_date_ending)
        FROM financial_ratios
        WHERE company_id = c.id AND period_type = 'ANNUAL'
    )
WHERE c.ticker IN ('AAPL', 'MSFT', 'GOOGL');
```

**Output example:**
| Ticker | ROE | ROE Percentile | Growth | Growth Percentile |
|--------|-----|---------------|--------|-------------------|
| AAPL | 145.3% | 95th | 5.2% | 30th |
| MSFT | 42.1% | 72nd | 15.2% | 85th |
| GOOGL| 27.3% | 58th | 13.4% | 78th |

This tells the user: "Apple's ROE is exceptional (top 5% in its industry) but revenue growth lags peers."

---

## 9.5 Industry Average Computation

### Java Implementation (cached in Redis)

```java
@Service
public class IndustryService {

    private final IndustryAverageRepository industryRepo;
    private final RedisTemplate<String, IndustryAveragesResponse> redisTemplate;

    @Cacheable(value = "industry::averages", key = "#sector + '::' + #industry")
    public IndustryAveragesResponse getAverages(String sector, String industry) {
        return industryRepo.findBySectorAndIndustry(sector, industry)
            .map(this::toResponse)
            .orElseGet(() -> computeDynamicAverages(sector, industry));
    }

    // Fallback: compute on-the-fly if materialized view not yet populated
    private IndustryAveragesResponse computeDynamicAverages(String sector, String industry) {
        return industryRepo.computeAveragesNative(sector, industry);
    }
}
```

### Native Query

```java
@Query(value = """
    SELECT
        c.sector,
        c.industry,
        COUNT(*) AS company_count,
        AVG(fr.pe_ratio) AS avg_pe,
        AVG(fr.roe) AS avg_roe,
        AVG(fr.revenue_growth_yoy) AS avg_revenue_growth,
        AVG(fr.net_margin) AS avg_net_margin,
        AVG(fr.debt_to_equity) AS avg_debt_to_equity,
        PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY fr.pe_ratio) AS pe_25th,
        PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY fr.pe_ratio) AS pe_median,
        PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY fr.pe_ratio) AS pe_75th
    FROM company c
    JOIN financial_ratios fr ON fr.company_id = c.id
        AND fr.period_type = 'ANNUAL'
        AND fr.fiscal_date_ending = (
            SELECT MAX(fiscal_date_ending) FROM financial_ratios
            WHERE company_id = c.id AND period_type = 'ANNUAL'
        )
    WHERE c.is_active = TRUE
      AND c.sector = :sector
      AND (:industry IS NULL OR c.industry = :industry)
    GROUP BY c.sector, c.industry
    """, nativeQuery = true)
IndustryAverageProjection computeAveragesNative(String sector, String industry);
```

---

## 9.6 Visual Comparison Mapping (Frontend)

| Metric Category | Chart Type | Color Coding |
|----------------|-----------|--------------|
| Valuation (P/E, PEG, P/B) | Horizontal bar chart | Lower = greener (cheaper) |
| Profitability (ROE, ROA, Margins) | Horizontal bar chart | Higher = greener (better) |
| Growth (Revenue, EPS, FCF) | Grouped bar chart | Higher = greener |
| Financial Health (D/E) | Single bar | Lower = greener |
| Dividends (Yield) | Single bar | Higher = greener |

Green/red relative coloring compares within the selected peer group, not absolute thresholds.

---

## 9.7 Performance Optimization

| Strategy | Detail |
|----------|--------|
| **Redis cache** | Comparison results cached per ticker-set hash (TTL: 1 hour) |
| **Batch loading** | Single query fetches all ratios for all compared companies |
| **Materialized view** | Industry averages pre-computed nightly |
| **Lazy loading** | Price charts loaded separately from metric comparison |
