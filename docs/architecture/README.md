# StockHub — Complete Architecture Specification

> **A production-quality financial research platform for US stocks**
>
> Target: Portfolio project demonstrating backend engineering, data engineering, and full-stack skills for Spring Boot roles.

---

## Document Index

| # | Section | File | Description |
|---|---------|------|-------------|
| 1 | **Product Architecture** | [01-product-architecture.md](01-product-architecture.md) | Target users, journeys, features, MVP scope, monetization, roadmap |
| 2 | **High-Level Architecture** | [02-high-level-architecture.md](02-high-level-architecture.md) | System diagram, component responsibilities, data flow, ETL workflow, tech stack |
| 3 | **Domain Modeling** | [03-domain-modeling.md](03-domain-modeling.md) | Core entities, relationships, service boundaries, entity definitions |
| 4 | **Database Design** | [04-database-design.md](04-database-design.md) | Complete PostgreSQL schema, ERD, indexes, constraints, TimescaleDB strategy, materialized views |
| 5 | **Data Engineering (ETL)** | [05-data-engineering.md](05-data-engineering.md) | Spring Batch job structure, provider abstraction, validation rules, scheduling |
| 6 | **Financial Metrics Engine** | [06-financial-metrics-engine.md](06-financial-metrics-engine.md) | 15+ ratio formulas, Java implementation, TTM calculations, edge cases |
| 7 | **Search System** | [07-search-system.md](07-search-system.md) | PostgreSQL FTS with tsvector, trigram similarity, Redis caching, ranking |
| 8 | **Stock Screener Engine** | [08-stock-screener-engine.md](08-stock-screener-engine.md) | Dynamic query builder, materialized view, pagination, 10+ filters |
| 9 | **Industry Comparison Engine** | [09-industry-comparison-engine.md](09-industry-comparison-engine.md) | Peer comparison, window functions, percentile ranking, visual mapping |
| 10 | **REST API Design** | [10-rest-api-design.md](10-rest-api-design.md) | 35+ endpoints, DTOs, validation, error handling (RFC 7807), pagination, rate limiting |
| 11 | **Authentication & Authorization** | [11-authentication-authorization.md](11-authentication-authorization.md) | JWT + refresh tokens, Google OAuth2, RBAC (FREE/PREMIUM/ADMIN), security best practices |
| 12 | **Redis Caching Strategy** | [12-redis-strategy.md](12-redis-strategy.md) | Cache keys, TTLs, market-aware expiry, cache warming, eviction, memory budget |
| 13 | **Frontend Architecture** | [13-frontend-architecture.md](13-frontend-architecture.md) | Angular project structure, NgRx Signal Store, routing, guards, interceptors |
| 14 | **Dashboard UI** | [14-dashboard-ui.md](14-dashboard-ui.md) | Component tree, layout wireframe, UX details, responsive breakpoints, loading/error states |
| 15 | **Watchlist System** | [15-watchlist-system.md](15-watchlist-system.md) | Service implementation, tier limits, batch price loading, alerts (post-MVP) |
| 16 | **Export System** | [16-export-system.md](16-export-system.md) | Excel (Apache POI) and PDF (iText) generation, async controller, performance limits |
| 17 | **Performance Optimization** | [17-performance-optimization.md](17-performance-optimization.md) | Query optimization, indexing, materialized views, Redis caching, connection pooling, benchmarks |
| 18 | **Testing Strategy** | [18-testing-strategy.md](18-testing-strategy.md) | Testing pyramid, unit/integration/API/E2E examples, Testcontainers, Playwright |
| 19 | **CI/CD Pipeline** | [19-cicd-pipeline.md](19-cicd-pipeline.md) | GitHub Actions workflows, Docker build, Azure/Vercel deployment, security scanning |
| 20 | **Deployment Architecture** | [20-deployment-architecture.md](20-deployment-architecture.md) | Infrastructure diagram, cost breakdown ($13/month), monitoring, backups, disaster recovery |
| 21 | **Resume Value** | [21-resume-value.md](21-resume-value.md) | Resume bullet points, interview talking points, system design answers, GitHub README template |

---

## Quick Reference

### Tech Stack

```
Angular 19 + Angular Material + NgRx Signal Store + Chart.js
                          │
                          ▼
              Spring Boot 3.2 Modular Monolith
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    PostgreSQL 16    TimescaleDB      Redis (Upstash)
     (Supabase)      (extension)
```

### Key Numbers

| Metric | Target |
|--------|--------|
| Companies (MVP) | S&P 500 |
| Historical data | 10 years |
| Concurrent users | 50 |
| Dashboard load | < 300ms |
| Search autocomplete | < 50ms |
| Screener response | < 500ms |
| Monthly cost | ~$13 |
| API endpoints | 35+ |
| Financial metrics | 15+ |

### Project Structure

```
d:\StockHub\
├── docs\
│   └── architecture\          ← You are here (21-section spec)
├── backend\                   ← Spring Boot (to be created)
│   ├── src\main\java\com\stockhub\
│   │   ├── auth\
│   │   ├── company\
│   │   ├── financials\
│   │   ├── prices\
│   │   ├── screener\
│   │   ├── comparison\
│   │   ├── watchlist\
│   │   ├── metrics\
│   │   ├── ingestion\
│   │   ├── export\
│   │   ├── cache\
│   │   └── common\
│   └── src\main\resources\
│       └── db\migration\      ← Flyway migrations
├── frontend\                  ← Angular 19 (to be created)
│   └── src\app\
│       ├── core\
│       ├── shared\
│       └── features\
│           ├── auth\
│           ├── dashboard\
│           ├── financials\
│           ├── screener\
│           ├── comparison\
│           ├── watchlist\
│           └── search\
└── docker-compose.yml         ← Local development
```

---

## Getting Started

### 1. Read the Architecture
Start with Section 1 (Product Architecture) and Section 2 (High-Level Architecture) for the big picture.

### 2. Understand the Data Model
Section 3 (Domain Modeling) and Section 4 (Database Design) define the data foundation.

### 3. Build Order
If implementing this project, follow the MVP scope from Section 1.6:

```
Phase 1: Auth → Company/Price data → Financial statements → Dashboard UI → Search
Phase 2: Basic Screener → Watchlist → Company Comparison → Financial Ratios → Growth Trends
Phase 3: Redis caching → Performance tuning → Testing suite → CI/CD
Phase 4: Exports → Industry Averages → Alerts → Premium gating
```

### 4. Interview Preparation
Section 21 (Resume Value) has ready-to-use resume bullet points and interview talking points.
