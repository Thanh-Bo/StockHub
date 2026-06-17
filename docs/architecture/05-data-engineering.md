# Section 5: Data Engineering (ETL Design)

## 5.1 ETL Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                     NIGHTLY ETL ORCHESTRATOR                         │
│                  FundamentalsIngestionJob (Spring Batch)             │
│                                                                      │
│  Job: nightlyFundamentalsIngestion                                  │
│  Trigger: @Scheduled(cron = "0 0 2 * * ?") — 2:00 AM ET daily      │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ STEP 1: Company Discovery (Reader)                            │  │
│  │   CompanyReader: SELECT * FROM company WHERE is_active = true │  │
│  │   Chunk size: 50 companies per chunk                          │  │
│  ├───────────────────────────────────────────────────────────────┤  │
│  │ STEP 2: Data Fetching (Processor — parallel, 5 threads)       │  │
│  │   SecEdgarItemProcessor: fetchAndParse(company)                │  │
│  │   ┌─────────────────────────────────────────────────────┐     │  │
│  │   │ FinancialDataProvider Interface                      │     │  │
│  │   │                                                     │     │  │
│  │   │ fetchIncomeStatements(ticker, years) → List<IS>      │     │  │
│  │   │ fetchBalanceSheets(ticker, years)     → List<BS>     │     │  │
│  │   │ fetchCashFlowStatements(ticker, years)→ List<CF>     │     │  │
│  │   │ fetchCompanyProfile(ticker)           → Company      │     │  │
│  │   │ fetchPriceHistory(ticker, years)      → List<Price>  │     │  │
│  │   └─────────────────────────────────────────────────┘     │     │
│  ├───────────────────────────────────────────────────────────────┤
│  │ STEP 3: Data Validation (Processor)                           │
│  │   FinancialDataValidator: @Valid annotations + custom rules  │
│  │   → Invalid records logged to validation_errors table        │
│  │   → Valid records proceed                                    │
│  ├───────────────────────────────────────────────────────────────┤
│  │ STEP 4: Data Transformation (Processor)                       │
│  │   FinancialDataTransformer: normalize units, tags, periods    │
│  │   • Convert all amounts to millions (USD)                     │
│  │   • Map XBRL tags to standard field names                     │
│  │   • Detect fiscal year boundaries                             │
│  │   • Handle restated filings (latest wins)                     │
│  ├───────────────────────────────────────────────────────────────┤
│  │ STEP 5: Upsert (Writer)                                       │
│  │   JdbcBatchItemWriter with ON CONFLICT UPDATE                 │
│  │   • income_statements: upsert by (company_id, date, period)   │
│  │   • balance_sheets:    upsert by (company_id, date, period)   │
│  │   • cash_flow_stmts:   upsert by (company_id, date, period)   │
│  ├───────────────────────────────────────────────────────────────┤
│  │ STEP 6: Metric Calculation (Processor → Writer)               │
│  │   MetricCalculationProcessor:                                 │
│  │   • For each company, calculate all ratios (Section 6)        │
│  │   • Upsert into financial_ratios table                        │
│  ├───────────────────────────────────────────────────────────────┤
│  │ STEP 7: Materialized View Refresh (Tasklet)                   │
│  │   MaterializedViewRefreshTasklet:                              │
│  │   → REFRESH MATERIALIZED VIEW CONCURRENTLY mv_screener_data   │
│  │   → REFRESH MATERIALIZED VIEW CONCURRENTLY mv_industry_avg    │
│  ├───────────────────────────────────────────────────────────────┤
│  │ STEP 8: Cache Warming (Tasklet)                                │
│  │   CacheWarmingTasklet:                                         │
│  │   → For top 100 companies by watchlist popularity:            │
│  │     → Compute dashboard response                              │
│  │     → Store in Redis: cache::dashboard::{ticker} (TTL: 24h)   │
│  │   → Compute and cache industry averages:                      │
│  │     → Redis: cache::industry::{sector}::{industry} (TTL: 24h) │
│  └───────────────────────────────────────────────────────────────┘
│                                                                      │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ SEPARATE JOB: PriceIngestionJob (daily, 6 PM ET market close) │  │
│  │   → YahooFinanceProvider.fetchPriceHistory(ticker, "1d")       │  │
│  │   → Upsert into stock_prices hypertable                        │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5.2 Spring Batch Job Configuration

### Job: `nightlyFundamentalsIngestion`

```java
package com.stockhub.ingestion.batch;

@Configuration
public class FundamentalsIngestionJobConfig {

    @Bean
    public Job nightlyFundamentalsIngestion(JobRepository jobRepository,
                                             Step companyDiscoveryStep,
                                             Step fetchFinancialDataStep,
                                             Step validateStep,
                                             Step transformStep,
                                             Step upsertStep,
                                             Step metricCalculationStep,
                                             Step materializedViewRefreshStep,
                                             Step cacheWarmingStep) {
        return new JobBuilder("nightlyFundamentalsIngestion", jobRepository)
            .start(companyDiscoveryStep)
            .next(fetchFinancialDataStep)
            .next(validateStep)
            .next(transformStep)
            .next(upsertStep)
            .next(metricCalculationStep)
            .next(materializedViewRefreshStep)
            .next(cacheWarmingStep)
            .build();
    }

    // --- Step 1: Company Discovery ---
    @Bean
    public Step companyDiscoveryStep(JobRepository jobRepository,
                                      PlatformTransactionManager txManager,
                                      CompanyRepository companyRepository) {
        return new StepBuilder("companyDiscovery", jobRepository)
            .<Company, Company>chunk(50, txManager)
            .reader(new RepositoryItemReaderBuilder<Company>()
                .repository(companyRepository)
                .methodName("findAllActive")
                .pageSize(50)
                .sorts(Map.of("ticker", Sort.Direction.ASC))
                .build())
            .processor(company -> company) // pass-through
            .writer(items -> { /* stored in execution context */ })
            .build();
    }

    // --- Step 2-4: Fetch, Validate, Transform (Composite Processor) ---
    @Bean
    public Step fetchFinancialDataStep(JobRepository jobRepository,
                                        PlatformTransactionManager txManager,
                                        FinancialDataProvider dataProvider,
                                        FinancialDataValidator validator,
                                        FinancialDataTransformer transformer) {
        return new StepBuilder("fetchFinancialData", jobRepository)
            .<Company, FinancialDataBundle>chunk(10, txManager) // smaller chunks for API calls
            .reader(companyReader())
            .processor(compositeProcessor(dataProvider, validator, transformer))
            .faultTolerant()
            .retryLimit(3)
            .retry(HttpServerErrorException.class)
            .skipLimit(50)
            .skip(DataFetchException.class)
            .listener(new LoggingSkipListener())
            .taskExecutor(taskExecutor()) // 5 threads parallel
            .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("etl-");
        executor.setConcurrencyLimit(5);
        return executor;
    }

    // --- Step 5: Upsert ---
    @Bean
    public Step upsertStep(JobRepository jobRepository,
                            PlatformTransactionManager txManager,
                            DataSource dataSource) {
        return new StepBuilder("upsert", jobRepository)
            .<FinancialDataBundle, FinancialDataBundle>chunk(50, txManager)
            .reader(/* read from previous step's output */)
            .writer(new JdbcBatchItemWriterBuilder<FinancialDataBundle>()
                .dataSource(dataSource)
                .sql("""
                    INSERT INTO income_statements (company_id, fiscal_date_ending, ...)
                    VALUES (?, ?, ...)
                    ON CONFLICT (company_id, fiscal_date_ending, period_type)
                    DO UPDATE SET total_revenue = EXCLUDED.total_revenue, ...
                    """)
                .itemPreparedStatementSetter(/* bind parameters */)
                .build())
            .build();
    }

    // --- Step 6: Metric Calculation ---
    @Bean
    public Step metricCalculationStep(JobRepository jobRepository,
                                       PlatformTransactionManager txManager,
                                       MetricCalculationService metricService) {
        return new StepBuilder("metricCalculation", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                List<String> tickers = (List<String>) chunkContext
                    .getStepContext().getJobExecutionContext().get("processedTickers");
                metricService.calculateAndPersistMetrics(tickers);
                return RepeatStatus.FINISHED;
            }, txManager)
            .build();
    }

    // --- Step 7: Materialized View Refresh ---
    @Bean
    public Step materializedViewRefreshStep(JobRepository jobRepository,
                                              PlatformTransactionManager txManager,
                                              JdbcTemplate jdbcTemplate) {
        return new StepBuilder("materializedViewRefresh", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_screener_data");
                jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_industry_averages");
                return RepeatStatus.FINISHED;
            }, txManager)
            .build();
    }

    // --- Step 8: Cache Warming ---
    @Bean
    public Step cacheWarmingStep(JobRepository jobRepository,
                                  PlatformTransactionManager txManager,
                                  CacheWarmingService cacheWarmingService) {
        return new StepBuilder("cacheWarming", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                cacheWarmingService.warmDashboardCache();
                cacheWarmingService.warmIndustryAveragesCache();
                return RepeatStatus.FINISHED;
            }, txManager)
            .build();
    }
}
```

---

## 5.3 FinancialDataProvider Abstraction

This is the key interface that allows swapping data sources without changing business logic.

```java
package com.stockhub.ingestion.provider;

public interface FinancialDataProvider {

    /**
     * Fetch company profile information.
     */
    CompanyProfileData fetchCompanyProfile(String ticker) throws DataFetchException;

    /**
     * Fetch historical daily prices.
     */
    List<StockPriceData> fetchPriceHistory(String ticker, LocalDate from, LocalDate to)
        throws DataFetchException;

    /**
     * Fetch income statements for the given years.
     */
    List<IncomeStatementData> fetchIncomeStatements(String ticker, int yearsBack)
        throws DataFetchException;

    /**
     * Fetch balance sheets for the given years.
     */
    List<BalanceSheetData> fetchBalanceSheets(String ticker, int yearsBack)
        throws DataFetchException;

    /**
     * Fetch cash flow statements for the given years.
     */
    List<CashFlowStatementData> fetchCashFlowStatements(String ticker, int yearsBack)
        throws DataFetchException;

    /**
     * Check if this provider supports the given ticker.
     */
    boolean supports(String ticker);

    /**
     * Provider identifier for logging/monitoring.
     */
    String getProviderName();
}
```

### Implementations

```java
// Primary: SEC EDGAR for fundamentals
@Component
@Primary
public class SecEdgarProvider implements FinancialDataProvider {
    // Uses SEC EDGAR API (https://efts.sec.gov/LATEST/search-index?q=...)
    // Parses XBRL filings for 10-K (annual) and 10-Q (quarterly)
    // Maps XBRL tags → standard field names
}

// Secondary: Yahoo Finance for prices and basic metrics
@Component
public class YahooFinanceProvider implements FinancialDataProvider {
    // Uses Yahoo Finance API (v8 or v11)
    // Fetches historical prices, market cap, basic ratios
    // Best for daily price data where EDGAR doesn't have it
}

// Future: FRED for macroeconomic data
@Component
public class FredProvider implements FinancialDataProvider {
    // Federal Reserve Economic Data
    // Interest rates, GDP growth, unemployment — for macro overlays
    // Post-MVP
}
```

---

## 5.4 Data Validation Rules

```java
package com.stockhub.ingestion.validation;

@Component
public class FinancialDataValidator {

    // Validation error → logged, record skipped
    public ValidationResult validate(IncomeStatementData data) {
        List<String> errors = new ArrayList<>();

        if (data.totalRevenue() == null) {
            errors.add("totalRevenue is required");
        }
        if (data.totalRevenue() != null && data.totalRevenue().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("totalRevenue cannot be negative: " + data.totalRevenue());
        }
        if (data.netIncome() != null && data.totalRevenue() != null
                && data.netIncome().abs().compareTo(data.totalRevenue().abs().multiply(BigDecimal.TEN)) > 0) {
            errors.add("netIncome exceeds 10x revenue — likely data error");
        }
        if (data.fiscalDateEnding() == null) {
            errors.add("fiscalDateEnding is required");
        }
        if (data.fiscalYear() < 2000 || data.fiscalYear() > Year.now().getValue() + 1) {
            errors.add("fiscalYear out of range: " + data.fiscalYear());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // Similar for BalanceSheetData, CashFlowStatementData, StockPriceData
}
```

---

## 5.5 Error Handling & Monitoring

### Validation Errors Table

```sql
CREATE TABLE validation_errors (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_name        VARCHAR(100) NOT NULL,
    ticker          VARCHAR(10),
    provider        VARCHAR(50),
    error_message   TEXT NOT NULL,
    raw_data_json   JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_val_errors_job ON validation_errors (job_name, created_at DESC);
```

### ETL Metrics (Logged + Prometheus)

- `etl_companies_processed` — Counter
- `etl_companies_failed` — Counter
- `etl_rows_upserted` — Counter (by statement type)
- `etl_duration_seconds` — Gauge
- `validation_errors_total` — Counter

---

## 5.6 Scheduled Triggers

```java
@Component
public class IngestionScheduler {

    private final JobLauncher jobLauncher;
    private final Job nightlyFundamentalsIngestion;
    private final Job priceIngestionJob;

    // Daily price update: 6:00 PM ET (after market close)
    @Scheduled(cron = "0 0 18 * * ?", zone = "America/New_York")
    public void runPriceIngestion() {
        jobLauncher.run(priceIngestionJob, new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters());
    }

    // Nightly fundamentals: 2:00 AM ET
    @Scheduled(cron = "0 0 2 * * ?", zone = "America/New_York")
    public void runFundamentalsIngestion() {
        jobLauncher.run(nightlyFundamentalsIngestion, new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters());
    }
}
```

---

## 5.7 Initial Data Seeding (Flyway Migration)

First data load is done via a Spring Batch job triggered manually after Flyway migrations:

```bash
# After first deployment
curl -X POST https://api.stockhub.com/api/v1/admin/ingestion/seed \
  -H "Authorization: Bearer {admin_token}" \
  -d '{"universe": "SP500", "yearsBack": 10}'
```

This triggers the same Spring Batch pipeline but for the full historical dataset — a one-time run that takes ~30-60 minutes for 500 companies × 10 years.
