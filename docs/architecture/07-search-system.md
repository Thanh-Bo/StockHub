# Section 7: Search System

## 7.1 Search Requirements

Users can search by:
1. **Ticker** — Exact or partial match (e.g., "AAPL", "aapl", "app")
2. **Company name** — Full-text search (e.g., "Apple", "Apple Inc", "Apple Computer")
3. **Fuzzy / typo-tolerant** — "Aple" → "Apple", "Googel" → "Google"

The search must feel instant — results within 100ms.

---

## 7.2 PostgreSQL Full-Text Search Design

### Strategy: Dual Approach

| Approach | Use Case | Technology |
|----------|----------|------------|
| **Trigram similarity** | Autocomplete, typo-tolerant ticker search | `pg_trgm` extension |
| **Full-text search** | Company name, keyword search | `tsvector` + `tsquery` |
| **Combined** | User types in a single search box | Union query with ranking |

### Why not Elasticsearch?
Not needed at this scale. PostgreSQL `tsvector` + `pg_trgm` handles 500-6,000 companies easily. Avoids infrastructure complexity.

---

## 7.3 Schema Setup

```sql
-- Enable extensions
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Add generated tsvector column
ALTER TABLE company
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('english', COALESCE(name, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(ticker, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(sector, '')), 'C') ||
    setweight(to_tsvector('english', COALESCE(industry, '')), 'C')
) STORED;

-- GIN index for tsvector
CREATE INDEX idx_company_search_vector ON company USING GIN (search_vector);

-- GIN indexes for trigram similarity
CREATE INDEX idx_company_ticker_trgm ON company USING GIN (ticker gin_trgm_ops);
CREATE INDEX idx_company_name_trgm ON company USING GIN (name gin_trgm_ops);
```

### Weight Explanation

| Weight | Field | Rationale |
|--------|-------|-----------|
| **A** (highest) | `ticker`, `name` | Exact/similar ticker and company name are most relevant |
| **B** | `description` | Business description keywords |
| **C** (lowest) | `sector`, `industry` | Broad category matching |

---

## 7.4 Search Query

```sql
-- Combined search: ticker trigram + full-text + name trigram
-- Ranked and limited to 10 results

SELECT
    c.id,
    c.ticker,
    c.name,
    c.sector,
    c.market_cap,
    ts_rank(c.search_vector, query) AS text_rank,
    similarity(c.ticker, :search_term) AS ticker_similarity,
    similarity(c.name, :search_term) AS name_similarity,
    (ts_rank(c.search_vector, query) * 1.0
     + similarity(c.ticker, :search_term) * 2.0
     + similarity(c.name, :search_term) * 1.5) AS combined_rank
FROM company c,
     plainto_tsquery('english', :search_term) query
WHERE c.is_active = TRUE
  AND (
      c.search_vector @@ query                           -- Full-text match
      OR similarity(c.ticker, :search_term) > 0.2        -- Ticker trigram
      OR similarity(c.name, :search_term) > 0.2          -- Name trigram
      OR UPPER(c.ticker) LIKE UPPER(:prefix_pattern)     -- Prefix match
  )
ORDER BY combined_rank DESC
LIMIT 10;
```

### Rank Formula Breakdown

```
combined_rank = (text_rank × 1.0) + (ticker_similarity × 2.0) + (name_similarity × 1.5)
```

- Ticker match is weighted highest (2.0) — "AAPL" should beat "Apple Hospitality REIT" when user types "appl"
- Name match is second (1.5) — natural language queries
- Text rank provides baseline relevance from description/sector

---

## 7.5 JPA Repository Implementation

```java
package com.stockhub.company;

@Repository
public interface CompanySearchRepository extends JpaRepository<Company, UUID> {

    // Native query for full-text + trigram search
    @Query(value = """
        SELECT c.*,
               ts_rank(c.search_vector, plainto_tsquery('english', :query)) AS text_rank,
               similarity(c.ticker, :query) AS ticker_sim,
               similarity(c.name, :query) AS name_sim
        FROM company c
        WHERE c.is_active = TRUE
          AND (
              c.search_vector @@ plainto_tsquery('english', :query)
              OR similarity(c.ticker, :query) > 0.2
              OR similarity(c.name, :query) > 0.2
              OR UPPER(c.ticker) LIKE UPPER(CONCAT(:query, '%'))
          )
        ORDER BY (ts_rank(c.search_vector, plainto_tsquery('english', :query)) * 1.0
                  + similarity(c.ticker, :query) * 2.0
                  + similarity(c.name, :query) * 1.5) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CompanySearchProjection> searchCompanies(@Param("query") String query,
                                                   @Param("limit") int limit);
}

// Projection to avoid loading full entities
public interface CompanySearchProjection {
    UUID getId();
    String getTicker();
    String getName();
    String getSector();
    BigDecimal getMarketCap();
    @JsonIgnore
    double getTextRank();
    @JsonIgnore
    double getTickerSim();
    @JsonIgnore
    double getNameSim();
}
```

---

## 7.6 Service Layer with Redis Caching

```java
package com.stockhub.company;

@Service
public class SearchService {

    private final CompanySearchRepository searchRepo;
    private final RedisTemplate<String, CompanySearchResponse> redisTemplate;

    // Autocomplete — for search-as-you-type (trigram-only, fast)
    @Cacheable(value = "search::autocomplete", key = "#query")
    public List<CompanySearchResponse> autocomplete(String query) {
        if (query == null || query.length() < 2) {
            return List.of();
        }

        // Only trigram search for autocomplete (faster than full-text)
        List<CompanySearchProjection> results = searchRepo.autocomplete(query, 8);
        return results.stream()
            .map(this::toResponse)
            .toList();
    }

    // Full search — for search results page
    @Cacheable(value = "search::full", key = "#query")
    public List<CompanySearchResponse> fullSearch(String query) {
        List<CompanySearchProjection> results = searchRepo.searchCompanies(query, 10);
        return results.stream()
            .map(this::toResponse)
            .toList();
    }

    private CompanySearchResponse toResponse(CompanySearchProjection proj) {
        return new CompanySearchResponse(
            proj.getId(),
            proj.getTicker(),
            proj.getName(),
            proj.getSector(),
            proj.getMarketCap()
        );
    }
}
```

---

## 7.7 Redis Cache Strategy for Search

| Cache Key | TTL | Purpose |
|-----------|-----|---------|
| `search::autocomplete::aapl` | 6 hours | Autocomplete results (high hit rate) |
| `search::full::apple inc` | 6 hours | Full search results |
| `popular::searches` | 24 hours | Trending searches for empty-state suggestions |

### Popular Searches (Redis Sorted Set)

```java
// Track search frequency
public void recordSearch(String query) {
    redisTemplate.opsForZSet().incrementScore("popular::searches", query.toLowerCase(), 1);
}

// Get trending searches
public List<String> getTrendingSearches(int limit) {
    Set<String> top = redisTemplate.opsForZSet()
        .reverseRange("popular::searches", 0, limit - 1);
    return new ArrayList<>(top);
}
```

---

## 7.8 Query Optimization

### Index Selection Analysis

```sql
-- For query: "apple" searching by trigram
EXPLAIN ANALYZE
SELECT * FROM company
WHERE similarity(ticker, 'apple') > 0.2
   OR similarity(name, 'apple') > 0.2
ORDER BY similarity(ticker, 'apple') DESC
LIMIT 10;

-- Expected: Bitmap Index Scan on idx_company_ticker_trgm
--           Bitmap Index Scan on idx_company_name_trgm
--           Combined with BitmapOr
-- Execution time: < 10ms for S&P 500 (< 50ms for Russell 3000)
```

### Performance Targets

| Dataset Size | Autocomplete | Full Search |
|-------------|--------------|-------------|
| S&P 500 (500 companies) | < 10ms | < 20ms |
| Russell 3000 (3,000) | < 20ms | < 50ms |
| With Redis cache hit | < 5ms | < 5ms |

---

## 7.9 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/search?q={query}&limit=10` | Full search (full-text + trigram) |
| `GET` | `/api/v1/search/autocomplete?q={query}` | Autocomplete (trigram only, 8 results) |
| `GET` | `/api/v1/search/trending` | Trending searches |

### Response Format

```json
{
  "results": [
    {
      "id": "uuid",
      "ticker": "AAPL",
      "name": "Apple Inc.",
      "sector": "Technology",
      "marketCap": 2800000000000,
      "matchType": "EXACT_TICKER"
    }
  ],
  "totalResults": 1,
  "query": "AAPL"
}
```
