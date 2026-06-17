# Section 2: High-Level Architecture

## 2.1 System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Angular 19 SPA (Vercel)                             │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │  │
│  │  │ Dashboard│ │ Screener │ │ Watchlist│ │ Compare  │ │  Search    │  │  │
│  │  │ Module   │ │ Module   │ │ Module   │ │ Module   │ │  Module    │  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────┘  │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────────────┐  │  │
│  │  │  Auth    │ │  Shared  │ │  Core    │ │  NgRx Signal Store       │  │  │
│  │  │  Module  │ │  Module  │ │  Module  │ │  (State Management)      │  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │ HTTPS (REST JSON)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             API GATEWAY LAYER                                │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │              Spring Boot 3.2 — Modular Monolith (Azure App Service)    │  │
│  │                                                                        │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │  │
│  │  │  Auth    │ │ Company  │ │Financials│ │ Screener │ │ Comparison │  │  │
│  │  │ Controller│ │Controller│ │Controller│ │Controller│ │ Controller │  │  │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────┬──────┘  │  │
│  │       │            │            │            │             │          │  │
│  │  ┌────┴────────────┴────────────┴────────────┴─────────────┴──────┐  │  │
│  │  │                    SERVICE LAYER                                │  │  │
│  │  │  AuthService  CompanyService  FinancialService  ScreenerService │  │  │
│  │  │  WatchlistService  ComparisonService  SearchService             │  │  │
│  │  │  ExportService  MetricCalculationService  AlertService          │  │  │
│  │  └────┬───────────────────────────────────────────────────────────┘  │  │
│  │       │                                                               │  │
│  │  ┌────┴───────────────────────────────────────────────────────────┐  │  │
│  │  │                 FINANCIAL DATA PROVIDER ABSTRACTION             │  │  │
│  │  │  FinancialDataProvider (interface)                              │  │  │
│  │  │    ├── SecEdgarProvider (primary — fundamentals)                │  │  │
│  │  │    ├── YahooFinanceProvider (prices, basic metrics)             │  │  │
│  │  │    └── FredProvider (macro data — future)                       │  │  │
│  │  └────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                        │  │
│  │  ┌────────────────────────────────────────────────────────────────┐  │  │
│  │  │                    BATCH / ETL LAYER                            │  │  │
│  │  │  Spring Batch Jobs:                                             │  │  │
│  │  │    ├── PriceIngestionJob (daily, 6 PM ET)                       │  │  │
│  │  │    ├── FundamentalsIngestionJob (nightly, 2 AM ET)              │  │  │
│  │  │    ├── MetricCalculationJob (after fundamentals)                │  │  │
│  │  │    └── CacheWarmingJob (after metrics)                          │  │  │
│  │  └────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                        │  │
│  │  ┌────────────────────────────────────────────────────────────────┐  │  │
│  │  │                    CACHING LAYER                                │  │  │
│  │  │  Redis Cache:                                                   │  │  │
│  │  │    ├── Company Profile Cache (TTL: 24h)                         │  │  │
│  │  │    ├── Dashboard Metrics Cache (TTL: 1h during market, 24h off) │  │  │
│  │  │    ├── Screener Results Cache (TTL: 1h)                         │  │  │
│  │  │    ├── Popular Search Cache (TTL: 6h)                           │  │  │
│  │  │    └── Industry Averages Cache (TTL: 24h)                       │  │  │
│  │  └────────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
          ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
          │  PostgreSQL   │ │ TimescaleDB  │ │    Redis     │
          │   (Supabase)  │ │  (extension) │ │  (Upstash)   │
          │               │ │              │ │              │
          │ • companies   │ │ • stock_     │ │ • Cache      │
          │ • financial_  │ │   prices     │ │ • Sessions   │
          │   statements  │ │              │ │ • Rate limit │
          │ • users       │ │              │ │              │
          │ • watchlists  │ │              │ │              │
          │ • screeners   │ │              │ │              │
          └──────────────┘ └──────────────┘ └──────────────┘
```

## 2.2 Component Responsibilities

### Frontend (Angular 19 — Vercel)

| Component | Responsibility |
|-----------|---------------|
| **AppComponent** | Root component, layout shell, navigation |
| **AuthModule** | Login, register, Google OAuth, JWT management, route guards |
| **DashboardModule** | Company dashboard: price chart, metrics cards, company profile |
| **FinancialsModule** | Income statement, balance sheet, cash flow tables with year/quarter toggle |
| **ScreenerModule** | Dynamic filter builder, results table with sort/paginate |
| **ComparisonModule** | Side-by-side company comparison with charts and tables |
| **WatchlistModule** | Create/edit watchlists, drag-drop reorder, price summaries |
| **SearchModule** | Global search bar with autocomplete, results page |
| **SharedModule** | Reusable components: MetricCard, DataTable, PriceChart, FilterBar |
| **CoreModule** | HTTP interceptors, auth guards, error handling, logging |

### Backend (Spring Boot 3.2 Modular Monolith — Azure App Service)

| Module (Package) | Responsibility |
|------------------|---------------|
| `com.stockhub.auth` | Registration, login, JWT creation/validation, refresh tokens, Google OAuth |
| `com.stockhub.company` | Company CRUD, profile, sector/industry classification, search |
| `com.stockhub.financials` | Income statements, balance sheets, cash flow statements, quarterly/annual |
| `com.stockhub.prices` | Historical stock prices, price charts, volume data |
| `com.stockhub.screener` | Dynamic query building, filter parsing, paginated screening results |
| `com.stockhub.comparison` | Multi-company aggregation, peer comparison calculations |
| `com.stockhub.watchlist` | Watchlist CRUD, stock associations, price summaries |
| `com.stockhub.metrics` | Financial ratio calculations, growth rate computations |
| `com.stockhub.ingestion` | Spring Batch ETL jobs, data provider integration |
| `com.stockhub.export` | Excel generation (Apache POI), PDF generation (iText) |
| `com.stockhub.cache` | Redis cache management, cache eviction policies |
| `com.stockhub.common` | Shared DTOs, exceptions, utilities, constants |

---

## 2.3 Data Flow

### Read Path (User Request → Response)

```
User clicks "AAPL Dashboard"
        │
        ▼
Angular: DashboardComponent.ngOnInit()
  → NgRx Signal Store dispatches: loadDashboard('AAPL')
  → DashboardService.getDashboard('AAPL')
  → HTTP GET /api/v1/companies/AAPL/dashboard
        │
        ▼
Spring Boot: CompanyController.getDashboard('AAPL')
  → Check Redis: cache::dashboard::AAPL
     ├── HIT → Return cached JSON ( < 10ms)
     └── MISS ↓
  → CompanyService.getDashboard('AAPL')
     ├── CompanyRepository.findByTicker('AAPL')       → companies table
     ├── PriceRepository.findLatest('AAPL')            → stock_prices (TimescaleDB)
     ├── FinancialRepository.findLatestMetrics('AAPL') → financial_metrics
     ├── WatchlistRepository.countUsersTracking('AAPL')→ watchlist_stocks
     └── IndustryService.getAverages('Technology')     → Redis or DB
  → Assemble DashboardResponse DTO
  → Store in Redis: cache::dashboard::AAPL (TTL: 1h market hours / 24h otherwise)
  → Return JSON
        │
        ▼
Angular: Update NgRx Signal Store → Render dashboard
```

### Write Path (ETL Ingestion)

```
[Scheduled: 2:00 AM ET daily]
        │
        ▼
Spring Batch: FundamentalsIngestionJob
  ┌──────────────────────────────────────────────┐
  │ Step 1: Read tickers to update               │
  │   → companyRepository.findAllActive()        │
  │   → Filter: last_updated < 12 hours          │
  ├──────────────────────────────────────────────┤
  │ Step 2: Fetch from SEC EDGAR                 │
  │   → SecEdgarProvider.fetchFundamentals(ticker)│
  │   → Parse XBRL/JSON response                 │
  ├──────────────────────────────────────────────┤
  │ Step 3: Validate                             │
  │   → Check required fields present            │
  │   → Validate numeric ranges                  │
  │   → Log validation failures                  │
  ├──────────────────────────────────────────────┤
  │ Step 4: Transform                            │
  │   → Normalize field names                    │
  │   → Convert to standard units (millions)     │
  │   → Calculate derived fields                 │
  ├──────────────────────────────────────────────┤
  │ Step 5: Store                                │
  │   → Upsert income_statements                 │
  │   → Upsert balance_sheets                    │
  │   → Upsert cash_flow_statements              │
  │   → Upsert company_metrics                   │
  ├──────────────────────────────────────────────┤
  │ Step 6: Calculate Derived Metrics            │
  │   → Revenue Growth (YoY, 3Y, 5Y, 10Y CAGR)  │
  │   → EPS Growth                               │
  │   → FCF Growth                               │
  │   → ROE, ROA, Debt/Equity, Margins, PEG      │
  │   → Store in financial_metrics table         │
  ├──────────────────────────────────────────────┤
  │ Step 7: Refresh Materialized Views           │
  │   → REFRESH MATERIALIZED VIEW mv_industry_avg │
  │   → REFRESH MATERIALIZED VIEW mv_screener     │
  ├──────────────────────────────────────────────┤
  │ Step 8: Warm Redis Cache                     │
  │   → Pre-compute popular company dashboards   │
  │   → Cache industry averages                  │
  │   → Cache screener presets                   │
  └──────────────────────────────────────────────┘
```

---

## 2.4 ETL Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     NIGHTLY ETL PIPELINE                         │
│                    (Trigger: 2:00 AM ET)                         │
│                                                                  │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌──────────────┐  │
│  │  SEC    │    │ Yahoo   │    │  FRED   │    │    RSS       │  │
│  │  EDGAR  │    │ Finance │    │  (TBD)  │    │   Feeds      │  │
│  └────┬────┘    └────┬────┘    └────┬────┘    └──────┬───────┘  │
│       │              │              │                │           │
│       └──────────────┴──────────────┴────────────────┘           │
│                          │                                       │
│                          ▼                                       │
│               ┌─────────────────────┐                            │
│               │  FinancialData      │                            │
│               │  Provider Interface │                            │
│               └─────────┬───────────┘                            │
│                         │                                        │
│                         ▼                                        │
│               ┌─────────────────────┐                            │
│               │  Data Validator     │                            │
│               │  (Bean Validation)  │                            │
│               └─────────┬───────────┘                            │
│                         │                                        │
│                         ▼                                        │
│               ┌─────────────────────┐                            │
│               │  Data Transformer   │                            │
│               │  (Normalization)    │                            │
│               └─────────┬───────────┘                            │
│                         │                                        │
│          ┌──────────────┼──────────────┐                         │
│          │              │              │                         │
│          ▼              ▼              ▼                         │
│  ┌──────────────┐ ┌──────────┐ ┌──────────────┐                 │
│  │  financial_  │ │ company_ │ │  stock_      │                 │
│  │  statements  │ │ metrics  │ │  prices      │                 │
│  └──────────────┘ └──────────┘ └──────────────┘                 │
│          │              │              │                         │
│          └──────────────┴──────────────┘                         │
│                         │                                        │
│                         ▼                                        │
│               ┌─────────────────────┐                            │
│               │  Metric Calculator  │                            │
│               │  (Growth, Ratios)   │                            │
│               └─────────┬───────────┘                            │
│                         │                                        │
│                         ▼                                        │
│               ┌─────────────────────┐                            │
│               │  Materialized       │                            │
│               │  View Refresh       │                            │
│               └─────────┬───────────┘                            │
│                         │                                        │
│                         ▼                                        │
│               ┌─────────────────────┐                            │
│               │  Redis Cache        │                            │
│               │  Warming            │                            │
│               └─────────────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2.5 API Request Lifecycle

```
Incoming HTTP Request
        │
        ▼
┌──────────────────┐
│  Filter Chain    │
│  ├── CorsFilter          (CORS headers)
│  ├── RateLimitFilter     (Redis-based rate limiting)
│  ├── JwtAuthFilter       (Token validation, SecurityContext)
│  └── RequestLoggingFilter(Structured logging)
└────────┬─────────┘
        │
        ▼
┌──────────────────┐
│  DispatcherServlet│
└────────┬─────────┘
        │
        ▼
┌──────────────────┐
│  Controller       │  ← Request DTO validation (@Valid)
│  (Thin layer)     │  ← Route to service
└────────┬─────────┘
        │
        ▼
┌──────────────────┐
│  Service          │  ← Business logic
│  (@Transactional) │  ← Orchestration
└────────┬─────────┘
        │
    ┌───┴───┐
    │       │
    ▼       ▼
┌──────┐ ┌──────┐
│Redis │ │  DB  │
│Cache │ │      │
└──┬───┘ └──┬───┘
   │        │
   └───┬────┘
       │
       ▼
┌──────────────────┐
│  Response DTO    │  ← Map entity → DTO
│  Assembly        │  ← @JsonView for field filtering
└────────┬─────────┘
        │
        ▼
┌──────────────────┐
│  Global Exception │  ← @ControllerAdvice
│  Handler          │  ← Standardized error response
└────────┬─────────┘
        │
        ▼
   HTTP Response
   (JSON)
```

---

## 2.6 Technology Stack Summary

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Frontend** | Angular | 19 | SPA framework |
| | Angular Material | 19 | UI component library |
| | NgRx Signal Store | 19 | State management |
| | Chart.js | 4.x | Interactive charts |
| | TypeScript | 5.x | Type-safe JavaScript |
| **Backend** | Java | 21 (LTS) | Language |
| | Spring Boot | 3.2 | Application framework |
| | Spring Security | 6.x | Authentication/authorization |
| | Spring Batch | 5.x | ETL job framework |
| | Spring Data JPA | 3.x | Data access |
| | Flyway | 10.x | Database migrations |
| | Hibernate | 6.x | ORM |
| **Database** | PostgreSQL | 16 | Primary database |
| | TimescaleDB | 2.x (extension) | Time-series (stock prices) |
| **Cache** | Redis | 7.x | Application cache + sessions |
| **Libraries** | Apache POI | 5.x | Excel generation |
| | iText | 8.x | PDF generation |
| | JJWT | 0.12.x | JWT token handling |
| | Lombok | 1.18.x | Boilerplate reduction |
| | MapStruct | 1.5.x | Entity ↔ DTO mapping |
| **DevOps** | Docker | 24.x | Containerization |
| | GitHub Actions | — | CI/CD |
| | Vercel | — | Frontend hosting |
| | Azure App Service | — | Backend hosting |
| | Supabase | — | Managed PostgreSQL |
| | Upstash | — | Managed Redis |
