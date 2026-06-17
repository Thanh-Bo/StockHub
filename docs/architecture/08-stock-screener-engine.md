# Section 8: Stock Screener Engine

## 8.1 Screener Filter Specifications

### MVP Filters (Free Tier)

| Filter | Type | Range | Example |
|--------|------|-------|---------|
| Market Cap | Range (min/max) | $0 - $3T+ | $10B - $500B |
| P/E Ratio | Range (min/max) | 0 - 1000+ | 10 - 30 |
| Revenue Growth (YoY) | Range (min/max) | -100% - 1000%+ | 15%+ |
| Dividend Yield | Range (min/max) | 0% - 20% | 2%+ |
| Sector | Multi-select | All GICS sectors | Technology, Healthcare |
| ROE | Range (min/max) | -500% - 500% | 15%+ |
| Debt to Equity | Range (min/max) | 0 - 100+ | 0 - 2.0 |
| Net Margin | Range (min/max) | -100% - 100% | 10%+ |

### Post-MVP Filters (Premium)

| Filter | Type |
|--------|------|
| PEG Ratio | Range |
| Price to Book | Range |
| Current Ratio | Range |
| EPS Growth (YoY) | Range |
| FCF Growth (YoY) | Range |
| Gross Margin | Range |
| Operating Margin | Range |
| Industry | Multi-select (granular) |
| Country | Select (future use) |
| Exchange | Select (future use) |

---

## 8.2 API Design

### Request

```
POST /api/v1/screener/search
```

```json
{
  "filters": [
    {
      "field": "marketCap",
      "operator": "BETWEEN",
      "minValue": "10000000000",
      "maxValue": "500000000000"
    },
    {
      "field": "peRatio",
      "operator": "BETWEEN",
      "minValue": "10",
      "maxValue": "30"
    },
    {
      "field": "revenueGrowthYoY",
      "operator": "GREATER_THAN_OR_EQUAL",
      "value": "15"
    },
    {
      "field": "sector",
      "operator": "IN",
      "values": ["Technology", "Healthcare"]
    },
    {
      "field": "roe",
      "operator": "GREATER_THAN_OR_EQUAL",
      "value": "15"
    }
  ],
  "sort": {
    "field": "marketCap",
    "direction": "DESC"
  },
  "pagination": {
    "page": 0,
    "size": 25
  }
}
```

### Response

```json
{
  "content": [
    {
      "ticker": "AAPL",
      "name": "Apple Inc.",
      "sector": "Technology",
      "industry": "Consumer Electronics",
      "marketCap": 2800000000000,
      "peRatio": 28.5,
      "revenueGrowthYoY": 5.2,
      "roe": 145.3,
      "dividendYield": 0.52,
      "debtToEquity": 1.95,
      "netMargin": 26.4,
      "price": 178.50,
      "matchesAllFilters": true
    }
  ],
  "totalElements": 23,
  "totalPages": 1,
  "page": 0,
  "size": 25
}
```

---

## 8.3 Dynamic Query Builder

The core challenge: dynamically constructing SQL WHERE clauses from arbitrary filter combinations without N+1 queries or ORM overhead.

### Approach: Criteria API + Specification Pattern

```java
package com.stockhub.screener;

@Component
public class ScreenerSpecificationBuilder {

    public Specification<Company> buildSpecification(List<FilterCriteria> filters) {
        return filters.stream()
            .map(this::toSpecification)
            .reduce(Specification::and)
            .orElse(Specification.where(null));
    }

    private Specification<Company> toSpecification(FilterCriteria filter) {
        return switch (filter.getField()) {
            case "marketCap" -> marketCapSpec(filter);
            case "sector"     -> sectorSpec(filter);
            // Financial ratio filters join to financial_ratios subquery
            case "peRatio", "revenueGrowthYoY", "roe",
                 "debtToEquity", "netMargin", "dividendYield",
                 "pegRatio", "priceToBook", "currentRatio",
                 "epsGrowthYoY", "fcfGrowthYoY",
                 "grossMargin", "operatingMargin" -> financialRatioSpec(filter);
            default -> throw new InvalidFilterException(filter.getField());
        };
    }

    private Specification<Company> financialRatioSpec(FilterCriteria filter) {
        return (root, query, cb) -> {
            // Subquery: find companies that have a recent financial ratio matching criteria
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<FinancialRatio> ratioRoot = subquery.from(FinancialRatio.class);

            Predicate companyJoin = cb.equal(
                ratioRoot.get("companyId"), root.get("id"));
            Predicate latestRatio = cb.equal(
                ratioRoot.get("fiscalDateEnding"),
                latestRatioDateSubquery(ratioRoot, cb)); // Most recent ratio

            Predicate valuePredicate = buildNumericPredicate(
                ratioRoot.get(filter.getField()), filter, cb);

            subquery.select(ratioRoot.get("companyId"))
                .where(cb.and(companyJoin, latestRatio, valuePredicate));

            return cb.exists(subquery);
        };
    }

    private Predicate buildNumericPredicate(Path<BigDecimal> path,
                                             FilterCriteria filter,
                                             CriteriaBuilder cb) {
        return switch (filter.getOperator()) {
            case GREATER_THAN -> cb.gt(path, filter.getValueAsBigDecimal());
            case GREATER_THAN_OR_EQUAL -> cb.ge(path, filter.getValueAsBigDecimal());
            case LESS_THAN -> cb.lt(path, filter.getValueAsBigDecimal());
            case LESS_THAN_OR_EQUAL -> cb.le(path, filter.getValueAsBigDecimal());
            case EQUAL -> cb.equal(path, filter.getValueAsBigDecimal());
            case BETWEEN -> cb.between(path,
                filter.getMinValueAsBigDecimal(),
                filter.getMaxValueAsBigDecimal());
        };
    }
}
```

### Optimized Approach: Materialized View (Production)

For better performance with large datasets, the screener queries the materialized view `mv_screener_data` directly, bypassing the JPA Criteria overhead:

```java
@Repository
public class ScreenerNativeRepository {

    private final JdbcTemplate jdbc;

    public ScreenerResult searchWithNativeQuery(List<FilterCriteria> filters,
                                                  SortCriteria sort,
                                                  int page, int size) {
        StringBuilder sql = new StringBuilder("""
            SELECT company_id, ticker, name, sector, industry,
                   market_cap, pe_ratio, revenue_growth_yoy,
                   roe, dividend_yield, debt_to_equity, net_margin
            FROM mv_screener_data
            WHERE 1=1
            """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        // Dynamically append WHERE clauses
        for (int i = 0; i < filters.size(); i++) {
            FilterCriteria f = filters.get(i);
            String paramName = "param" + i;

            switch (f.getField()) {
                case "marketCap" -> appendRangeClause(sql, "market_cap", f, params);
                case "peRatio"   -> appendRangeClause(sql, "pe_ratio", f, params);
                case "revenueGrowthYoY" -> appendRangeClause(sql, "revenue_growth_yoy", f, params);
                case "sector"    -> appendInClause(sql, "sector", f, params);
                case "roe"       -> appendRangeClause(sql, "roe", f, params);
                case "debtToEquity" -> appendRangeClause(sql, "debt_to_equity", f, params);
                case "netMargin" -> appendRangeClause(sql, "net_margin", f, params);
                case "dividendYield" -> appendRangeClause(sql, "dividend_yield", f, params);
            }
        }

        // Sorting (whitelist validation to prevent SQL injection)
        String safeSortField = validateSortField(sort.getField());
        String safeDirection = "ASC".equalsIgnoreCase(sort.getDirection()) ? "ASC" : "DESC";
        sql.append(" ORDER BY ").append(safeSortField).append(" ").append(safeDirection);

        // Pagination
        sql.append(" LIMIT :limit OFFSET :offset");
        params.addValue("limit", size);
        params.addValue("offset", page * size);

        return executeQuery(sql.toString(), params);
    }

    // Whitelist sort fields to prevent SQL injection
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "ticker", "name", "sector", "market_cap", "pe_ratio",
        "revenue_growth_yoy", "roe", "dividend_yield",
        "debt_to_equity", "net_margin"
    );

    private String validateSortField(String field) {
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new InvalidSortFieldException(field);
        }
        return field;
    }
}
```

---

## 8.4 Pagination & Sorting

```java
public record ScreenerRequest(
    @Valid @NotEmpty List<FilterCriteria> filters,
    @Valid SortCriteria sort,
    @Valid Pagination pagination
) {}

public record FilterCriteria(
    @NotBlank String field,
    @NotNull FilterOperator operator,
    String value,
    String minValue,
    String maxValue,
    List<String> values
) {}

public record SortCriteria(
    @NotBlank String field,
    @Pattern(regexp = "ASC|DESC") String direction
) {}

public record Pagination(
    @Min(0) int page,
    @Min(1) @Max(100) int size
) {}

public enum FilterOperator {
    EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL,
    LESS_THAN, LESS_THAN_OR_EQUAL, BETWEEN, IN
}
```

---

## 8.5 Performance Optimization

### Strategy

| Optimization | Technique | Impact |
|-------------|-----------|--------|
| **Materialized View** | Pre-joined company + latest ratios | Eliminates subquery overhead |
| **Covering Indexes** | Indexes on all filterable columns in MV | Index-only scans |
| **Redis Cache** | Cache common filter combinations | Skip DB entirely for popular screens |
| **Pagination** | Keyset pagination for deep scrolling | Avoids offset drift |
| **Query Timeout** | 5-second timeout on screener queries | Circuit-breaker for bad queries |

### Redis Caching for Screeners

```java
@Cacheable(value = "screener::results",
           key = "{#request.hashCode()}",
           unless = "#result.totalElements == 0")
public ScreenerResponse search(ScreenerRequest request) {
    return screenerRepo.searchWithNativeQuery(
        request.filters(), request.sort(),
        request.pagination().page(), request.pagination().size());
}
```

Cache key is computed from the hash of filter+sort+pagination — same search yields cache hit.

### Keyset Pagination (for deep scrolling)

```sql
-- Instead of OFFSET (which gets slow at high offsets):
SELECT * FROM mv_screener_data
WHERE market_cap > :lastMarketCap  -- keyset column
ORDER BY market_cap DESC
LIMIT 25;
```

---

## 8.6 Count Query Optimization

Counting total matching rows is expensive. Two strategies:

**Strategy 1: Estimate (default for large universes)**
```sql
-- Use PostgreSQL's estimate from the materialized view
SELECT reltuples::bigint AS estimate
FROM pg_class
WHERE relname = 'mv_screener_data';
```

**Strategy 2: Exact count (small result sets)**
```sql
SELECT COUNT(*) FROM mv_screener_data WHERE [filters];
```

Hybrid approach: Run exact count. If it takes > 2 seconds, fall back to estimate and show "~23 results."

---

## 8.7 Response Cache Invalidation

Screener results change only when:
1. Nightly ETL updates financial ratios
2. Stock prices change (affects P/E, P/B, etc.)

Invalidation strategy: After nightly ETL completes, flush all `screener::*` cache keys.

```java
@Scheduled(cron = "0 30 3 * * ?") // 3:30 AM ET (after ETL)
public void invalidateScreenerCache() {
    Set<String> keys = redisTemplate.keys("screener::*");
    if (!keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.info("Invalidated {} screener cache entries", keys.size());
    }
}
```
