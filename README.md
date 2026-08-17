# StockHub — US Stock Financial Analysis Platform

> A full-stack financial research platform for US stocks, built with Java 21, Spring Boot 4.1, Angular 19, PostgreSQL, and Redis.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-red)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## Overview

StockHub lets retail investors research US stocks with company fundamentals, financial statements, stock screening, peer comparisons, and watchlists. It ingests SEC EDGAR data nightly, calculates 15+ financial metrics, and serves dashboards in under 300ms.

### Features

- 🔍 **Search** — Full-text search by ticker or company name with sub-50ms autocomplete
- 📊 **Company Dashboard** — Price charts, key metrics, financial statements, peer comparison
- 📈 **Financial Statements** — Income, Balance Sheet, Cash Flow (annual & quarterly, up to 10 years)
- 🧮 **Financial Metrics** — 15+ calculated ratios (ROE, ROA, PEG, CAGR, D/E, margins)
- 🔬 **Stock Screener** — Filter by 8+ criteria with materialized view optimization
- ⚖️ **Company Comparison** — Side-by-side peer comparison with industry percentile ranking
- 📋 **Watchlists** — Create, reorder, and monitor stocks with real-time price updates
- 🔐 **Authentication** — JWT with refresh token rotation, Google OAuth, RBAC (Free/Premium/Admin)
- 📥 **Export** — Excel (Apache POI) and PDF (iText) generation

---

## Architecture

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

**Full 21-section architecture specification** → [`docs/architecture/`](docs/architecture/)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 4.1, Spring Security, Spring Batch, Spring Data JPA, Flyway |
| **Frontend** | Angular 19, Angular Material, NgRx Signal Store, Chart.js |
| **Database** | PostgreSQL 16, TimescaleDB |
| **Cache** | Redis (Upstash) |
| **ETL** | Spring Batch, SEC EDGAR, Yahoo Finance (pluggable providers) |
| **Export** | Apache POI (Excel), iText (PDF) |
| **DevOps** | Docker, GitHub Actions, Azure App Service, Vercel |
| **Testing** | JUnit 5, Mockito, Testcontainers, Playwright |

---

## Quick Start

### Prerequisites

- **Java 21** ([Eclipse Temurin](https://adoptium.net/))
- **Maven 3.9+** ([Download](https://maven.apache.org/download.cgi)) — or use `mvnw` (run `mvn wrapper:wrapper` first)
- **PostgreSQL 16** ([Download](https://www.postgresql.org/download/windows/))
- **Node.js 22+** ([Download](https://nodejs.org/))

### 1. Clone

```bash
git clone https://github.com/Thanh-Bo/StockHub.git
cd StockHub
```

### 2. Install PostgreSQL 16

Download and install [PostgreSQL 16 for Windows](https://www.postgresql.org/download/windows/).

During installation, set:
- **Port**: `5432`
- **Superuser password**: `stockhub`

Then create the database:

```bash
# Open psql (from Start Menu → PostgreSQL 16 → psql)
psql -U postgres
```

```sql
CREATE USER stockhub WITH PASSWORD 'stockhub';
CREATE DATABASE stockhub OWNER stockhub;
GRANT ALL PRIVILEGES ON DATABASE stockhub TO stockhub;
\q
```

> **Note**: TimescaleDB is not available on Windows. For local dev, we use the `local` Spring profile which auto-creates tables from JPA entities instead of Flyway migrations.

### 3. Start Backend

```powershell
cd backend

# IMPORTANT: PowerShell requires quoting the -D argument
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Or with IntelliJ / VS Code: set the active profile to `local`.

The backend starts on **`http://localhost:8080`**. Tables are auto-created from JPA entities on startup.

> **Redis is optional** for local development. The app works without it — endpoints return data directly from PostgreSQL. If you want caching, you can skip it for now.

### 4. Start Frontend

```bash
cd frontend
npm install
npm start
```

Open **`http://localhost:4200`** in your browser.

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/companies/{ticker}` | Company profile |
| `GET` | `/api/v1/companies/{ticker}/dashboard` | Full dashboard |
| `GET` | `/api/v1/companies/{ticker}/prices` | Historical prices |
| `GET` | `/api/v1/companies/{ticker}/metrics` | Financial ratios |
| `POST` | `/api/v1/screener/search` | Stock screener |
| `POST` | `/api/v1/companies/compare` | Company comparison |
| `GET` | `/api/v1/search?q=AAPL` | Full-text search |
| `GET` | `/api/v1/watchlists` | User watchlists |
| `POST` | `/api/v1/auth/register` | Register |
| `POST` | `/api/v1/auth/login` | Login |

Full API docs → [`docs/architecture/10-rest-api-design.md`](docs/architecture/10-rest-api-design.md)

---

## Project Structure

```
StockHub/
├── backend/                          # Spring Boot 4.1
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/stockhub/
│       ├── auth/                     # JWT, OAuth, Security
│       ├── cache/                    # Redis config, TTLs, warming
│       ├── common/                   # DTOs, exceptions, enums
│       ├── company/                  # Company profile, search
│       ├── comparison/               # Peer comparison, industry averages
│       ├── export/                   # Excel & PDF generation
│       ├── financials/               # Financial statements
│       ├── ingestion/                # ETL providers, batch jobs
│       ├── metrics/                  # Financial ratio calculations
│       ├── prices/                   # Historical price data
│       ├── screener/                 # Dynamic stock screener
│       └── watchlist/                # Watchlist management
├── frontend/                         # Angular 19
│   └── src/app/
│       ├── core/                     # Auth, interceptors, guards
│       ├── shared/                   # Reusable components & pipes
│       └── features/
│           ├── auth/                 # Login & register
│           ├── dashboard/            # Company dashboard
│           ├── financials/           # Financial statements
│           ├── screener/             # Stock screener
│           ├── comparison/           # Company comparison
│           ├── watchlist/            # Watchlists
│           └── search/               # Search results
├── docs/architecture/                # 21-section architecture spec
└── docker-compose.yml                # PostgreSQL + Redis
```

---

## Configuration

Copy `.env.example` to `.env` and adjust:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/stockhub` |
| `DB_USERNAME` | Database user | `stockhub` |
| `DB_PASSWORD` | Database password | `stockhub` |
| `REDIS_URL` | Redis connection URL | `redis://localhost:6379` |
| `JWT_PRIVATE_KEY` | RSA private key (auto-generated if empty) | — |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | — |
| `CORS_ORIGINS` | Allowed frontend origins | `http://localhost:4200` |

---

## Performance

| Operation | Target |
|-----------|--------|
| Dashboard load | < 300ms |
| Search autocomplete | < 50ms |
| Screener (S&P 500) | < 500ms |

Optimizations: Redis multi-tier caching, materialized views, TimescaleDB hypertables, covering indexes, connection pooling.

---

## Testing

```bash
# Backend unit tests
cd backend && ./mvnw test

# Backend integration tests (requires Docker)
cd backend && ./mvnw verify -pl integration-tests

# Frontend tests
cd frontend && npm test
```

---

## Deployment

- **Backend**: Azure App Service (B1, ~$13/month)
- **Frontend**: Vercel (Free)
- **Database**: Supabase PostgreSQL (Free)
- **Redis**: Upstash (Free)
- **CI/CD**: GitHub Actions

See [`docs/architecture/20-deployment-architecture.md`](docs/architecture/20-deployment-architecture.md) for details.

---

## License

MIT © 2026 [Thanh-Bo](https://github.com/Thanh-Bo)
