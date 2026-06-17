# Section 3: Domain Modeling

## 3.1 Core Entities

### Entity Relationship Overview

```
User ───────< Watchlist >─────── WatchlistStock ───────> Company
  │                                      │
  │                                      │
  └──────────────────────────────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    │                    │                    │
                    ▼                    ▼                    ▼
              StockPrice           IncomeStatement      BalanceSheet
              (TimescaleDB)                              │
                                                         ▼
                                                  CashFlowStatement
                                                         │
                                                         ▼
                                                   FinancialRatio
                                                         │
                                                         ▼
                                              CompanyComparison
                                                         │
                                                         ▼
                                                    Industry
```

---

## 3.2 Entity Definitions

### Company
The central entity representing a publicly traded US company.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `ticker` | String(10) | Stock ticker symbol (e.g., AAPL) — UNIQUE |
| `cik` | String(10) | SEC Central Index Key |
| `name` | String(255) | Full company name |
| `description` | Text | Business description |
| `sector` | String(100) | Sector classification (e.g., Technology) |
| `industry` | String(100) | Industry classification (e.g., Consumer Electronics) |
| `employees` | Integer | Number of employees |
| `foundedYear` | Integer | Year founded |
| `headquarters` | String(255) | HQ location |
| `website` | String(255) | Company website |
| `logoUrl` | String(500) | Company logo URL |
| `marketCap` | BigDecimal | Market capitalization |
| `isActive` | Boolean | Whether actively traded |
| `createdAt` | Instant | Record creation timestamp |
| `updatedAt` | Instant | Last update timestamp |

**Relationships**:
- Has many `StockPrice` (1:N)
- Has many `IncomeStatement` (1:N)
- Has many `BalanceSheet` (1:N)
- Has many `CashFlowStatement` (1:N)
- Has many `FinancialRatio` (1:N)
- Has many `WatchlistStock` (1:N)
- Belongs to one `Industry` (N:1)

---

### StockPrice
Historical daily price data. Stored in TimescaleDB hypertable.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `companyId` | UUID | FK → Company |
| `date` | Date | Trading date |
| `open` | BigDecimal | Opening price |
| `high` | BigDecimal | Day high |
| `low` | BigDecimal | Day low |
| `close` | BigDecimal | Closing price |
| `adjustedClose` | BigDecimal | Adjusted close (splits/dividends) |
| `volume` | Long | Trading volume |

**Partition Key**: `date` (TimescaleDB chunking: 1 month intervals)

**Indexes**:
- `(company_id, date DESC)` — Primary query pattern
- `(date)` — Date range scans

---

### IncomeStatement
Quarterly and annual income statements from SEC filings.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `companyId` | UUID | FK → Company |
| `fiscalDateEnding` | Date | Period end date |
| `periodType` | Enum | `ANNUAL` or `QUARTERLY` |
| `fiscalYear` | Integer | Fiscal year |
| `fiscalQuarter` | Integer | Fiscal quarter (null for annual) |
| `totalRevenue` | BigDecimal | Total revenue |
| `costOfRevenue` | BigDecimal | COGS |
| `grossProfit` | BigDecimal | Gross profit |
| `operatingExpense` | BigDecimal | Total operating expenses |
| `operatingIncome` | BigDecimal | Operating income (EBIT) |
| `netIncome` | BigDecimal | Net income |
| `eps` | BigDecimal | Earnings per share (basic) |
| `epsDiluted` | BigDecimal | Diluted EPS |
| `interestExpense` | BigDecimal | Interest expense |
| `incomeTaxExpense` | BigDecimal | Income tax |
| `ebitda` | BigDecimal | EBITDA |
| `reportUrl` | String(500) | Link to SEC filing |
| `filingDate` | Date | SEC filing date |
| `createdAt` | Instant | Record creation |

**Constraints**:
- `UNIQUE(company_id, fiscal_date_ending, period_type)` — No duplicate statements

**Indexes**:
- `(company_id, fiscal_date_ending DESC)` — Company financials lookup
- `(company_id, period_type, fiscal_year DESC)` — Annual/quarterly listings

---

### BalanceSheet

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `companyId` | UUID | FK → Company |
| `fiscalDateEnding` | Date | Period end date |
| `periodType` | Enum | `ANNUAL` or `QUARTERLY` |
| `fiscalYear` | Integer | Fiscal year |
| `fiscalQuarter` | Integer | Fiscal quarter |
| `totalAssets` | BigDecimal | Total assets |
| `totalCurrentAssets` | BigDecimal | Current assets |
| `cashAndEquivalents` | BigDecimal | Cash & short-term investments |
| `totalLiabilities` | BigDecimal | Total liabilities |
| `totalCurrentLiabilities` | BigDecimal | Current liabilities |
| `longTermDebt` | BigDecimal | Long-term debt |
| `totalDebt` | BigDecimal | Total debt |
| `totalShareholderEquity` | BigDecimal | Shareholder equity |
| `retainedEarnings` | BigDecimal | Retained earnings |
| `treasuryStock` | BigDecimal | Treasury stock |
| `sharesOutstanding` | Long | Common shares outstanding |
| `reportUrl` | String(500) | Link to SEC filing |
| `filingDate` | Date | SEC filing date |

---

### CashFlowStatement

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `companyId` | UUID | FK → Company |
| `fiscalDateEnding` | Date | Period end date |
| `periodType` | Enum | `ANNUAL` or `QUARTERLY` |
| `fiscalYear` | Integer | Fiscal year |
| `fiscalQuarter` | Integer | Fiscal quarter |
| `operatingCashFlow` | BigDecimal | Cash from operations |
| `capitalExpenditure` | BigDecimal | CapEx |
| `freeCashFlow` | BigDecimal | FCF (= operating CF - CapEx) |
| `cashFlowFromInvesting` | BigDecimal | Investing activities |
| `cashFlowFromFinancing` | BigDecimal | Financing activities |
| `dividendsPaid` | BigDecimal | Dividends paid |
| `stockIssuance` | BigDecimal | Stock issuance (net) |
| `debtIssuance` | BigDecimal | Debt issuance (net) |
| `netChangeInCash` | BigDecimal | Net change in cash |
| `reportUrl` | String(500) | Link to SEC filing |
| `filingDate` | Date | SEC filing date |

---

### FinancialRatio
Calculated metrics — derived, not ingested.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `companyId` | UUID | FK → Company |
| `fiscalDateEnding` | Date | Period end date |
| `periodType` | Enum | `ANNUAL` or `QUARTERLY` |
| `revenueGrowthYoY` | BigDecimal | Year-over-year revenue growth % |
| `revenueGrowth3Y` | BigDecimal | 3-year CAGR revenue % |
| `revenueGrowth5Y` | BigDecimal | 5-year CAGR revenue % |
| `epsGrowthYoY` | BigDecimal | YoY EPS growth % |
| `fcfGrowthYoY` | BigDecimal | YoY FCF growth % |
| `roe` | BigDecimal | Return on Equity % |
| `roa` | BigDecimal | Return on Assets % |
| `debtToEquity` | BigDecimal | Debt-to-Equity ratio |
| `grossMargin` | BigDecimal | Gross margin % |
| `operatingMargin` | BigDecimal | Operating margin % |
| `netMargin` | BigDecimal | Net margin % |
| `peRatio` | BigDecimal | Price-to-Earnings ratio |
| `pegRatio` | BigDecimal | PEG ratio |
| `dividendYield` | BigDecimal | Dividend yield % |
| `priceToBook` | BigDecimal | P/B ratio |
| `currentRatio` | BigDecimal | Current ratio |

---

### Industry
Sector and industry classifications.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `sector` | String(100) | Sector name |
| `industryGroup` | String(100) | Industry group |
| `industry` | String(100) | Specific industry |
| `companyCount` | Integer | Number of companies (materialized) |
| `avgMarketCap` | BigDecimal | Average market cap (materialized) |
| `avgPE` | BigDecimal | Average P/E (materialized) |
| `avgRevenueGrowth` | BigDecimal | Average revenue growth (materialized) |

---

### User

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `email` | String(255) | Email — UNIQUE |
| `passwordHash` | String(255) | BCrypt hash |
| `firstName` | String(100) | First name |
| `lastName` | String(100) | Last name |
| `role` | Enum | `FREE`, `PREMIUM`, `ADMIN` |
| `authProvider` | Enum | `LOCAL`, `GOOGLE` |
| `googleId` | String(255) | Google OAuth ID (nullable) |
| `emailVerified` | Boolean | Email verification status |
| `createdAt` | Instant | Registration date |
| `lastLoginAt` | Instant | Last login timestamp |
| `isActive` | Boolean | Account active flag |

---

### Watchlist

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `userId` | UUID | FK → User |
| `name` | String(100) | Watchlist name |
| `description` | String(500) | Optional description |
| `isDefault` | Boolean | Default watchlist for user |
| `sortOrder` | Integer | Display order |
| `createdAt` | Instant | Creation date |
| `updatedAt` | Instant | Last modified |

**Constraint**: Free users max 1 watchlist, Premium users unlimited (enforced in service layer).

---

### WatchlistStock

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `watchlistId` | UUID | FK → Watchlist |
| `companyId` | UUID | FK → Company |
| `addedAt` | Instant | When added |
| `sortOrder` | Integer | Position in watchlist |

**Constraint**: Free users max 10 stocks per watchlist; `UNIQUE(watchlist_id, company_id)`.

---

### Alert (Post-MVP)

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `userId` | UUID | FK → User |
| `companyId` | UUID | FK → Company |
| `alertType` | Enum | `PRICE_ABOVE`, `PRICE_BELOW`, `EARNINGS_DATE` |
| `thresholdValue` | BigDecimal | Trigger value |
| `isActive` | Boolean | Alert enabled |
| `lastTriggeredAt` | Instant | Last trigger date |
| `createdAt` | Instant | Creation date |

---

### SavedScreener (Post-MVP)

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `userId` | UUID | FK → User |
| `name` | String(100) | Saved screener name |
| `filtersJson` | JSONB | Filter configuration |
| `sortField` | String(50) | Default sort field |
| `sortDirection` | String(4) | ASC/DESC |
| `createdAt` | Instant | Creation date |
| `updatedAt` | Instant | Last modified |

---

## 3.3 Entity Relationship Summary

```
User (1) ────< (N) Watchlist (1) ────< (N) WatchlistStock (N) >──── (1) Company
                                                                        │
                                                                        │ (1)
                          ┌─────────────────────────────────────────────┤
                          │ (N)          │ (N)          │ (N)          │ (N)
                          ▼              ▼              ▼              ▼
                    StockPrice    IncomeStatement  BalanceSheet  CashFlowStatement
                    (TimescaleDB)
                          │              │              │              │
                          └──────────────┴──────────────┘              │
                                         │                              │
                                         ▼                              ▼
                                   FinancialRatio              ┌──────────────┐
                                                             │   Derived    │
                                                             │  from all 3  │
                                                             │  statements  │
                                                             └──────────────┘

Company (N) >──── (1) Industry
Company (N) >────< (N) CompanyComparison (Many-to-Many, with metadata)
User (1) ────< (N) Alert (N) >──── (1) Company  [Post-MVP]
User (1) ────< (N) SavedScreener  [Post-MVP]
```

---

## 3.4 Domain Service Boundaries

| Service | Owns | Depends On |
|---------|------|------------|
| `CompanyService` | Company, Industry | — |
| `PriceService` | StockPrice | CompanyService |
| `FinancialService` | IncomeStatement, BalanceSheet, CashFlowStatement | CompanyService |
| `MetricCalculationService` | FinancialRatio | FinancialService, PriceService |
| `ScreenerService` | Query logic (stateless) | CompanyService, MetricCalculationService |
| `ComparisonService` | CompanyComparison (ephemeral) | FinancialService, MetricCalculationService |
| `WatchlistService` | Watchlist, WatchlistStock | CompanyService, PriceService |
| `AuthService` | User, JWT | — |
| `AlertService` | Alert (Post-MVP) | CompanyService, PriceService |
| `ExportService` | Excel, PDF generation | FinancialService, MetricCalculationService |
| `SearchService` | Full-text search queries | CompanyService |
| `IndustryService` | Industry aggregation | CompanyService, MetricCalculationService |
