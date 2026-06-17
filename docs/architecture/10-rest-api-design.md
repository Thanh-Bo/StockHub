# Section 10: REST API Design

## 10.1 API Design Principles

| Principle | Implementation |
|-----------|---------------|
| **Versioning** | URI path versioning (`/api/v1/`) |
| **Consistent naming** | Plural nouns for collections, kebab-case for multi-word resources |
| **HATEOAS** | Not implemented (overkill for SPA; use OpenAPI documentation instead) |
| **Pagination** | Page-based with `page` and `size` query params; consistent envelope |
| **Error format** | RFC 7807 Problem Details (`application/problem+json`) |
| **Field filtering** | `@JsonView` for sparse responses (e.g., `?view=summary` vs `?view=detailed`) |
| **Rate limiting** | Redis-based token bucket; headers: `X-RateLimit-Remaining` |

---

## 10.2 Complete Endpoint Catalog

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` | No | Register with email/password |
| `POST` | `/api/v1/auth/login` | No | Login, returns JWT + refresh token |
| `POST` | `/api/v1/auth/refresh` | No | Refresh expired access token |
| `POST` | `/api/v1/auth/logout` | Yes | Invalidate refresh token |
| `GET` | `/api/v1/auth/me` | Yes | Get current user profile |
| `GET` | `/api/v1/auth/oauth2/google` | No | Google OAuth2 redirect |

---

### Companies

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/companies` | No | List companies (paginated) |
| `GET` | `/api/v1/companies/{ticker}` | No | Get company profile |
| `GET` | `/api/v1/companies/{ticker}/dashboard` | No | Get full dashboard (aggregated) |
| `GET` | `/api/v1/companies/{ticker}/profile` | No | Company profile detail |

---

### Financial Statements

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/companies/{ticker}/income-statements` | No | Income statements (annual/quarterly) |
| `GET` | `/api/v1/companies/{ticker}/balance-sheets` | No | Balance sheets |
| `GET` | `/api/v1/companies/{ticker}/cash-flow-statements` | No | Cash flow statements |

**Query Params for financial statements:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `period` | `ANNUAL` / `QUARTERLY` | `ANNUAL` | Statement periodicity |
| `years` | Integer | 5 | Number of years to return |
| `page` | Integer | 0 | Page number |
| `size` | Integer | 10 | Page size |

---

### Stock Prices

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/companies/{ticker}/prices` | No | Historical prices |
| `GET` | `/api/v1/companies/{ticker}/prices/latest` | No | Latest price snapshot |

**Query Params for prices:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `range` | `1M` / `3M` / `6M` / `1Y` / `5Y` / `MAX` | `1Y` | Time range |
| `interval` | `1d` / `1wk` / `1mo` | `1d` | Data granularity |

---

### Financial Metrics & Ratios

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/companies/{ticker}/metrics` | No | Latest financial ratios |
| `GET` | `/api/v1/companies/{ticker}/metrics/history` | No | Historical ratio trends |

---

### Screener

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/screener/search` | No | Run screener with filters |
| `GET` | `/api/v1/screener/filters` | No | Available filter metadata |
| `POST` | `/api/v1/screener/saved` | Yes | Save screener configuration (PREMIUM) |
| `GET` | `/api/v1/screener/saved` | Yes | List saved screeners |
| `DELETE` | `/api/v1/screener/saved/{id}` | Yes | Delete saved screener |

---

### Comparison

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/companies/compare` | No | Compare 2-5 companies |

---

### Industry

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/industries` | No | List industries |
| `GET` | `/api/v1/industries/{sector}/{industry}/averages` | No | Industry averages |

---

### Search

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/search` | No | Full-text search |
| `GET` | `/api/v1/search/autocomplete` | No | Autocomplete (lightweight) |
| `GET` | `/api/v1/search/trending` | No | Trending searches |

**Query Params:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `q` | String | — | Search query |
| `limit` | Integer | 10 | Max results |

---

### Watchlist

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/watchlists` | Yes | List user's watchlists |
| `POST` | `/api/v1/watchlists` | Yes | Create watchlist |
| `GET` | `/api/v1/watchlists/{id}` | Yes | Get watchlist with stocks |
| `PUT` | `/api/v1/watchlists/{id}` | Yes | Update watchlist name/description |
| `DELETE` | `/api/v1/watchlists/{id}` | Yes | Delete watchlist |
| `POST` | `/api/v1/watchlists/{id}/stocks` | Yes | Add stock to watchlist |
| `DELETE` | `/api/v1/watchlists/{id}/stocks/{ticker}` | Yes | Remove stock from watchlist |
| `PUT` | `/api/v1/watchlists/{id}/stocks/reorder` | Yes | Reorder stocks |

---

### Exports (PREMIUM only)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/exports/excel` | Yes | Generate Excel export |
| `POST` | `/api/v1/exports/pdf` | Yes | Generate PDF export |

---

### Admin

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/admin/ingestion/seed` | ADMIN | Trigger initial data seeding |
| `POST` | `/api/v1/admin/ingestion/trigger` | ADMIN | Manually trigger nightly ETL |
| `GET` | `/api/v1/admin/ingestion/status` | ADMIN | ETL job status |
| `GET` | `/api/v1/admin/users` | ADMIN | List all users |
| `PUT` | `/api/v1/admin/users/{id}/role` | ADMIN | Change user role |

---

## 10.3 Request/Response DTOs

### Common Patterns

```java
// --- Pagination Envelope (wraps all list responses) ---
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {}

// --- Error Response (RFC 7807) ---
public record ProblemDetail(
    String type,        // URI to error type documentation
    String title,       // Human-readable summary
    int status,         // HTTP status code
    String detail,      // Detailed explanation
    String instance,    // URI of the request that caused the error
    Map<String, List<String>> errors  // Field-level validation errors
) {}

// --- Dashboard Response (most complex DTO) ---
public record DashboardResponse(
    // Company profile
    String ticker,
    String name,
    String description,
    String sector,
    String industry,
    String headquarters,
    BigDecimal marketCap,
    Long employees,

    // Price data (latest day)
    BigDecimal currentPrice,
    BigDecimal priceChange,
    BigDecimal priceChangePercent,
    BigDecimal dayHigh,
    BigDecimal dayLow,
    BigDecimal previousClose,
    Long volume,

    // Price chart data (1 year daily)
    List<PricePoint> priceHistory,

    // Key metrics
    BigDecimal revenueGrowthYoY,
    BigDecimal epsGrowthYoY,
    BigDecimal roe,
    BigDecimal roa,
    BigDecimal peRatio,
    BigDecimal grossMargin,
    BigDecimal netMargin,
    BigDecimal debtToEquity,
    BigDecimal dividendYield,

    // Industry context
    IndustryContext industryContext,

    // Metadata
    Instant lastUpdated,
    String dataSource
) {}

public record PricePoint(
    LocalDate date,
    BigDecimal close,
    BigDecimal adjustedClose,
    Long volume
) {}

public record IndustryContext(
    String sector,
    String industry,
    BigDecimal avgPE,
    BigDecimal avgROE,
    BigDecimal avgRevenueGrowth,
    BigDecimal avgNetMargin,
    BigDecimal pePercentile,     // Where this company ranks
    BigDecimal roePercentile
) {}
```

---

## 10.4 Validation Rules

```java
// --- Registration ---
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName
) {}

// --- Login ---
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

// --- Screener ---
public record ScreenerRequest(
    @NotEmpty @Size(max = 10) List<@Valid FilterCriteria> filters,
    @Valid SortCriteria sort,
    @Valid PaginationRequest pagination
) {}

public record FilterCriteria(
    @NotBlank String field,
    @NotNull FilterOperator operator,
    @Digits(integer = 20, fraction = 4) String value,
    @Digits(integer = 20, fraction = 4) String minValue,
    @Digits(integer = 20, fraction = 4) String maxValue,
    @Size(max = 20) List<String> values
) {}

// --- Comparison ---
public record ComparisonRequest(
    @NotEmpty @Size(min = 2, max = 5) List<@Pattern(regexp = "^[A-Z]{1,5}$") String> tickers,
    List<String> metrics,
    boolean includeIndustryAverages
) {}
```

---

## 10.5 Error Handling Strategy

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCompanyNotFound(CompanyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ProblemDetail.builder()
                .type("https://api.stockhub.com/errors/company-not-found")
                .title("Company not found")
                .status(404)
                .detail(ex.getMessage())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> fieldErrors = ex.getBindingResult()
            .getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                FieldError::getField,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
            ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ProblemDetail.builder()
                .type("https://api.stockhub.com/errors/validation-failed")
                .title("Validation failed")
                .status(400)
                .detail("One or more fields are invalid")
                .errors(fieldErrors)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ProblemDetail.builder()
                .type("https://api.stockhub.com/errors/internal-error")
                .title("Internal server error")
                .status(500)
                .detail("An unexpected error occurred")
                .build());
    }
}
```

### Error Response Examples

**404 — Company Not Found:**
```json
{
  "type": "https://api.stockhub.com/errors/company-not-found",
  "title": "Company not found",
  "status": 404,
  "detail": "Company with ticker 'ZZZZ' not found in our database. It may not be in the S&P 500."
}
```

**400 — Validation Failed:**
```json
{
  "type": "https://api.stockhub.com/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "errors": {
    "tickers": ["Must contain between 2 and 5 tickers"],
    "tickers[2]": ["must match \"^[A-Z]{1,5}$\""]
  }
}
```

**429 — Rate Limited:**
```json
{
  "type": "https://api.stockhub.com/errors/rate-limited",
  "title": "Too many requests",
  "status": 429,
  "detail": "Rate limit exceeded. Try again in 60 seconds."
}
```

---

## 10.6 Versioning Strategy

```
/api/v1/...  → Current stable (MVP)
/api/v2/...  → Future breaking changes

Versioning policy:
- New fields added to response?        No version bump (backward compatible)
- New optional query params?           No version bump
- Field removed or renamed?            New version
- Response structure changed?          New version
- Old version deprecated?              Sunset after 6 months with notice header
```

Deprecation header:
```
Sunset: Sat, 31 Dec 2026 23:59:59 GMT
Deprecation: true
Link: </api/v2/companies>; rel="successor-version"
```

---

## 10.7 Pagination Strategy

### Page-Based (Current)

```
GET /api/v1/companies?page=0&size=25
```

Response includes `totalElements`, `totalPages` for UI pagination controls.

### Keyset Pagination (Future, for high-offset queries)

```
GET /api/v1/screener/search?after=1700000000000&size=25
```

Uses `marketCap` as cursor. Avoids offset drift when new data arrives.

### Pagination Rules

| Rule | Value |
|------|-------|
| Default page size | 25 |
| Max page size | 100 |
| Max pages | 200 (5,000 records max via offset) |
| Deep pagination | Use keyset for offset > 200 |

---

## 10.8 Rate Limiting Headers

Every API response includes:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1687459200
```

**Limits by tier:**
| Tier | Requests/minute |
|------|----------------|
| Anonymous | 30 |
| FREE | 60 |
| PREMIUM | 300 |
| ADMIN | 1000 |

Implemented with Redis token bucket:
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) {
        String key = "rate_limit:" + getClientIdentifier(request);
        Long remaining = redisTemplate.opsForValue().decrement(key);

        if (remaining != null && remaining < 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(getTTL(key)));
            return;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining != null ? remaining : 0)));
        chain.doFilter(request, response);
    }
}
```
