# Section 17: Performance Optimization

## 17.1 Performance Targets

| Operation | Target (p95) | Strategy |
|-----------|-------------|----------|
| Dashboard load | < 300ms | Redis cache + materialized views + optimized queries |
| Search autocomplete | < 50ms | Trigram index + Redis |
| Full search | < 100ms | tsvector GIN index |
| Screener (S&P 500) | < 500ms | Materialized view + covering indexes |
| Screener (Russell 3000) | < 1s | Materialized view + Redis cache |
| Company comparison | < 200ms | Batch query + Redis |
| Price chart (1Y daily) | < 100ms | TimescaleDB chunk pruning |
| Financial statements (10yr) | < 150ms | Composite indexes |

---


## 17.2 Query Optimization

### Before (Naive — N+1 Problem)

```java
// BAD: Loads prices one at a time
watchlist.getStocks().forEach(stock -> {
    BigDecimal price = priceRepo.findLatestByCompany(stock.getCompanyId());
    // 50 stocks = 50 queries!
});
```

### After (Optimized — Batch Loading)

```java
// GOOD: Single query with DISTINCT ON
List<UUID> companyIds = stocks.stream().map(WatchlistStock::getCompanyId).toList();
Map<UUID, BigDecimal> prices = priceRepo.findLatestPrices(companyIds);
// 50 stocks = 1 query
```

### Dashboard Query Optimization

```sql
-- Single query to fetch all dashboard data (company + latest ratio + latest price)
SELECT
    c.ticker, c.name, c.sector, c.industry, c.market_cap,
    fr.roe, fr.pe_ratio, fr.revenue_growth_yoy, fr.net_margin,
    fr.gross_margin, fr.debt_to_equity, fr.dividend_yield,
    sp.close AS current_price,
    prev.close AS previous_close,
    (sp.close - prev.close) / prev.close * 100 AS price_change_pct
FROM company c
LEFT JOIN LATERAL (
    SELECT * FROM financial_ratios
    WHERE company_id = c.id AND period_type = 'ANNUAL'
    ORDER BY fiscal_date_ending DESC LIMIT 1
) fr ON TRUE
LEFT JOIN LATERAL (
    SELECT close, date FROM stock_prices
    WHERE company_id = c.id
    ORDER BY date DESC LIMIT 1
) sp ON TRUE
LEFT JOIN LATERAL (
    SELECT close FROM stock_prices
    WHERE company_id = c.id AND date < sp.date
    ORDER BY date DESC LIMIT 1
) prev ON TRUE
WHERE c.ticker = 'AAPL';
```

**Execution plan**: 3 index scans + 1 nested loop. Expected: < 5ms for single company.

---

## 17.3 Indexing Strategy (Summary)

### Critical Indexes

```sql
-- Companies: search + filter
CREATE INDEX idx_company_ticker ON company (ticker);                        -- Lookup
CREATE INDEX idx_company_search ON company USING GIN (search_vector);       -- Full-text
CREATE INDEX idx_company_ticker_trgm ON company USING GIN (ticker gin_trgm_ops); -- Autocomplete
CREATE INDEX idx_company_market_cap ON company (market_cap DESC);           -- Screener sort

-- Prices: time-series query
CREATE INDEX idx_prices_company_date ON stock_prices (company_id, date DESC); -- Primary access

-- Financials: company + date
CREATE INDEX idx_income_company_date ON income_statements (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_ratios_company_date ON financial_ratios (company_id, fiscal_date_ending DESC);
```

### Covering Indexes for Screener

```sql
-- Materialized view indexes for filter + sort without heap access
CREATE INDEX idx_mv_screener_pe ON mv_screener_data (pe_ratio);
CREATE INDEX idx_mv_screener_roe ON mv_screener_data (roe DESC);
CREATE INDEX idx_mv_screener_rev ON mv_screener_data (revenue_growth_yoy DESC);
CREATE INDEX idx_mv_screener_mc ON mv_screener_data (market_cap DESC);
CREATE INDEX idx_mv_screener_sector ON mv_screener_data (sector);
```

---

## 17.4 Materialized Views

### When to Use

| Scenario | Solution |
|----------|----------|
| Screener data (company + latest ratios) | `mv_screener_data` — refreshed nightly |
| Industry averages (aggregate query) | `mv_industry_averages` — refreshed nightly |
| Popular watchlist summaries | Not a MV; warmed in Redis after ETL |

### Refresh Timing

- `mv_screener_data`: Refreshed after financial statement upsert completes (~3:00 AM ET)
- `mv_industry_averages`: Refreshed after metric calculation completes (~3:15 AM ET)
- Both use `REFRESH MATERIALIZED VIEW CONCURRENTLY` (non-blocking reads)

---

## 17.5 Redis Caching Strategy

### Decision Matrix: What to Cache

| Data | Cache? | TTL | Reason |
|------|--------|-----|--------|
| Company profile | ✅ Yes | 24h | Rarely changes, high read |
| Dashboard (popular) | ✅ Yes | 1h / 24h | Complex query, high read |
| Dashboard (unpopular) | ❌ No | — | Low hit rate, DB is fast enough |
| Financial statements | ✅ Yes | 24h | Once-a-day data, expensive query |
| Screener results | ✅ Yes | 1h | Expensive query, common repeats |
| Search autocomplete | ✅ Yes | 6h | High frequency, low cardinality |
| Industry averages | ✅ Yes | 24h | Computed once nightly |
| Latest stock price | ✅ Yes | 5 min | High frequency, shared |
| Price history (1Y) | ✅ Yes | 1h | Large result set, shared across views |

### Cache Invalidation Triggers

| Trigger | Keys Invalidated |
|---------|-----------------|
| ETL completion (3:30 AM) | `screener::*`, `comparison::*`, `industry::*`, `financials::*` |
| Market close (4:00 PM) | `prices::*::latest`, `company::dashboard::*` |
| User modifies watchlist | `watchlist::{userId}` |

---

## 17.6 Application-Level Optimizations

### Connection Pooling (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # 20 concurrent DB connections
      minimum-idle: 5
      connection-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### HTTP Compression

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
    min-response-size: 1024
```

### JSON Serialization

- Use `@JsonView` for sparse responses — don't send fields the client doesn't need
- Dashboard: ~50 fields sent
- Screener list: ~10 fields per company
- Search autocomplete: 4 fields per result

### Lazy Loading in Frontend

```
Dashboard loads in stages:
  1. Company header + key metrics (immediate from Redis) → 50ms
  2. Price chart (lazy, Chart.js renders progressively) → 100ms
  3. Financial statement tabs (loaded on tab click, not on page load)
  4. Peer comparison widget (loaded after dashboard renders)
```

---

## 17.7 Database Configuration

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50                     # Batch INSERT/UPDATE
        order_inserts: true
        order_updates: true
        default_batch_fetch_size: 100        # Batch @ManyToOne fetches
    open-in-view: false                       # Don't hold session for view rendering

  flyway:
    enabled: true
    locations: classpath:db/migration
```

### PostgreSQL Tuning (Supabase / Managed)

```sql
-- Recommended settings for portfolio scale
shared_buffers = 256MB
effective_cache_size = 768MB
work_mem = 16MB
maintenance_work_mem = 64MB
random_page_cost = 1.1        -- SSD
effective_io_concurrency = 200
```

---

## 17.8 Performance Testing

### Key Scenarios to Benchmark

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PerformanceTest {

    @Test
    void dashboardShouldLoadUnder300ms() {
        Stopwatch sw = Stopwatch.createStarted();
        DashboardResponse response = dashboardService.getDashboard("AAPL");
        long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);

        assertThat(response).isNotNull();
        assertThat(elapsed).isLessThan(300);
    }

    @Test
    void searchAutocompleteShouldRespondUnder50ms() {
        Stopwatch sw = Stopwatch.createStarted();
        List<CompanySearchResponse> results = searchService.autocomplete("appl");
        long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);

        assertThat(results).isNotEmpty();
        assertThat(elapsed).isLessThan(50);
    }

    @Test
    void screenerWith5FiltersShouldRespondUnder500ms() {
        ScreenerRequest request = buildComplexFilterRequest();
        Stopwatch sw = Stopwatch.createStarted();
        ScreenerResponse response = screenerService.search(request);
        long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);

        assertThat(response.totalElements()).isGreaterThan(0);
        assertThat(elapsed).isLessThan(500);
    }
}
```
