# Section 4: Database Design

## 4.1 Entity Relationship Diagram (ERD)

```
┌──────────────────────┐       ┌──────────────────────┐
│        users         │       │       industry       │
├──────────────────────┤       ├──────────────────────┤
│ id            UUID PK│       │ id            UUID PK│
│ email     VARCHAR UNQ│       │ sector      VARCHAR  │
│ password    VARCHAR  │       │ industry_grp VARCHAR │
│ first_name  VARCHAR  │       │ industry    VARCHAR  │
│ last_name   VARCHAR  │       │ company_cnt INTEGER  │
│ role          ENUM   │       │ avg_mkt_cap NUMERIC  │
│ auth_provider ENUM   │       │ avg_pe      NUMERIC  │
│ google_id   VARCHAR  │       │ avg_rev_gr  NUMERIC  │
│ email_verified BOOL  │       └──────────┬───────────┘
│ created_at TIMESTAMP │                  │
│ last_login TIMESTAMP │                  │ (1)
│ is_active   BOOLEAN  │                  │
└──────────┬───────────┘                  │
           │ (1)                          │
           │                              ▼
           │                   ┌──────────────────────┐
           │                   │       company        │
           │                   ├──────────────────────┤
           │                   │ id            UUID PK│
           │                   │ ticker    VARCHAR UNQ│
           │                   │ cik        VARCHAR   │
           │         ┌─────────│ name        VARCHAR  │
           │         │         │ description   TEXT   │
           │         │         │ sector      VARCHAR  │
           │         │         │ industry    VARCHAR  │
           │         │         │ employees   INTEGER  │
           │         │         │ founded_yr  INTEGER  │
           │         │         │ hq          VARCHAR  │
           │         │         │ website     VARCHAR  │
           │         │         │ logo_url    VARCHAR  │
           │         │         │ market_cap  NUMERIC  │
           │         │         │ is_active   BOOLEAN  │
           │         │         │ created_at TIMESTAMP │
           │         │         │ updated_at TIMESTAMP │
           │         │         │ industry_id  UUID FK │
           │         │         └──────────┬───────────┘
           │         │                    │
           │         │    ┌───────────────┼───────────────┬─────────────────┐
           │         │    │ (1)           │ (1)           │ (1)             │ (1)
           │         │    ▼               ▼               ▼                 ▼
           │         │ ┌──────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
           │         │ │  stock_  │ │   income_    │ │   balance_   │ │  cash_flow_  │
           │         │ │  prices  │ │  statements  │ │   sheets     │ │  statements  │
           │         │ ├──────────┤ ├──────────────┤ ├──────────────┤ ├──────────────┤
           │         │ │id UUID PK│ │id     UUID PK│ │id     UUID PK│ │id     UUID PK│
           │         │ │company_id│ │company_id FK │ │company_id FK │ │company_id FK │
           │         │ │date DATE │ │fiscal_date   │ │fiscal_date   │ │fiscal_date   │
           │         │ │open      │ │period ENUM   │ │period ENUM   │ │period ENUM   │
           │         │ │high      │ │fiscal_year   │ │fiscal_year   │ │fiscal_year   │
           │         │ │low       │ │fiscal_qtr    │ │fiscal_qtr    │ │fiscal_qtr    │
           │         │ │close     │ │total_revenue │ │total_assets  │ │operating_cf  │
           │         │ │adj_close │ │gross_profit  │ │total_liab    │ │capex         │
           │         │ │volume    │ │oper_income   │ │long_term_debt│ │free_cash_flow│
           │         │ │          │ │net_income    │ │shareholder_eq│ │dividends     │
           │         │ │          │ │eps           │ │shares_out    │ │net_chg_cash  │
           │         │ │          │ │ebitda        │ │cash_equiv    │ │filing_date   │
           │         │ │          │ │filing_date   │ │filing_date   │ │              │
           │         │ │TIMESCALE │ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
           │         │ │HYPERTABLE│       │                │                │
           │         │ └──────────┘       └────────┬───────┴────────┬───────┘
           │         │                            │                │
           │         │                            ▼                │
           │         │                   ┌──────────────────────┐   │
           │         │                   │   financial_ratios   │   │
           │         │                   ├──────────────────────┤   │
           │         │                   │ id            UUID PK│   │
           │         │                   │ company_id     UUID FK│  │
           │         │                   │ fiscal_date     DATE  │   │
           │         │                   │ period_type      ENUM │   │
           │         │                   │ rev_growth_yoy NUMERIC│   │
           │         │                   │ rev_growth_3y  NUMERIC│   │
           │         │                   │ rev_growth_5y  NUMERIC│   │
           │         │                   │ eps_growth_yoy NUMERIC│   │
           │         │                   │ fcf_growth_yoy NUMERIC│   │
           │         │                   │ roe            NUMERIC│   │
           │         │                   │ roa            NUMERIC│   │
           │         │                   │ debt_to_equity NUMERIC│   │
           │         │                   │ gross_margin   NUMERIC│   │
           │         │                   │ oper_margin    NUMERIC│   │
           │         │                   │ net_margin     NUMERIC│   │
           │         │                   │ pe_ratio       NUMERIC│   │
           │         │                   │ peg_ratio      NUMERIC│   │
           │         │                   │ div_yield      NUMERIC│   │
           │         │                   │ price_to_book  NUMERIC│   │
           │         │                   │ current_ratio  NUMERIC│   │
           │         │                   └──────────────────────┘   │
           │         │                                              │
           │  ┌──────┴──────┐                                       │
           │  │  watchlist  │                                       │
           │  ├─────────────┤                                       │
           │  │id    UUID PK│                                       │
           │  │user_id  FK  │                                       │
           │  │name VARCHAR │                                       │
           │  │description  │                                       │
           │  │is_default   │                                       │
           │  │sort_order   │                                       │
           │  │created_at   │                                       │
           │  │updated_at   │                                       │
           │  └──────┬──────┘                                       │
           │         │ (1)                                          │
           │         ▼                                              │
           │  ┌──────────────┐                                      │
           │  │ watchlist_   │                                      │
           │  │   stock      │                                      │
           │  ├──────────────┤                                      │
           │  │id    UUID PK │                                      │
           │  │watchlist_id FK│                                     │
           │  │company_id  FK│◄─────────────────────────────────────┘
           │  │added_at      │
           │  │sort_order    │
           │  └──────────────┘
           │
           ▼
    ┌──────────────┐        ┌──────────────────┐
    │   alerts     │        │  saved_screeners │
    │ (Post-MVP)   │        │   (Post-MVP)     │
    ├──────────────┤        ├──────────────────┤
    │id     UUID PK│        │id         UUID PK│
    │user_id   FK  │        │user_id       FK  │
    │company_id FK │        │name      VARCHAR │
    │alert_type    │        │filters     JSONB │
    │threshold     │        │sort_field VARCHAR │
    │is_active     │        │sort_dir   VARCHAR │
    │last_triggered│        │created_at        │
    │created_at    │        │updated_at        │
    └──────────────┘        └──────────────────┘
```

---

## 4.2 Complete PostgreSQL Schema

### 4.2.1 Extensions

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- Trigram search
CREATE EXTENSION IF NOT EXISTS timescaledb;       -- Time-series
```

### 4.2.2 Enums

```sql
CREATE TYPE user_role AS ENUM ('FREE', 'PREMIUM', 'ADMIN');
CREATE TYPE auth_provider AS ENUM ('LOCAL', 'GOOGLE');
CREATE TYPE period_type AS ENUM ('ANNUAL', 'QUARTERLY');
CREATE TYPE alert_type AS ENUM ('PRICE_ABOVE', 'PRICE_BELOW', 'EARNINGS_DATE');
```

### 4.2.3 Tables

```sql
-- ============================================================
-- INDUSTRY
-- ============================================================
CREATE TABLE industry (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sector          VARCHAR(100) NOT NULL,
    industry_group  VARCHAR(100) NOT NULL,
    industry        VARCHAR(100) NOT NULL,
    company_count   INTEGER DEFAULT 0,
    avg_market_cap  NUMERIC(20, 2),
    avg_pe_ratio    NUMERIC(10, 2),
    avg_revenue_growth NUMERIC(10, 4),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_industry UNIQUE (sector, industry_group, industry)
);

-- ============================================================
-- COMPANY
-- ============================================================
CREATE TABLE company (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticker          VARCHAR(10) NOT NULL,
    cik             VARCHAR(10),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    sector          VARCHAR(100),
    industry        VARCHAR(100),
    employees       INTEGER,
    founded_year    INTEGER,
    headquarters    VARCHAR(255),
    website         VARCHAR(255),
    logo_url        VARCHAR(500),
    market_cap      NUMERIC(20, 2),
    industry_id     UUID REFERENCES industry(id),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_company_ticker UNIQUE (ticker),
    CONSTRAINT uq_company_cik UNIQUE (cik)
);

-- ============================================================
-- STOCK_PRICES (TimescaleDB Hypertable)
-- ============================================================
CREATE TABLE stock_prices (
    id              UUID DEFAULT uuid_generate_v4(),
    company_id      UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    date            DATE NOT NULL,
    open            NUMERIC(12, 4),
    high            NUMERIC(12, 4),
    low             NUMERIC(12, 4),
    close           NUMERIC(12, 4),
    adjusted_close  NUMERIC(12, 4),
    volume          BIGINT,

    CONSTRAINT uq_price_date UNIQUE (company_id, date)
);

-- Convert to TimescaleDB hypertable
SELECT create_hypertable('stock_prices', 'date', chunk_time_interval => INTERVAL '1 month');

-- ============================================================
-- INCOME_STATEMENTS
-- ============================================================
CREATE TABLE income_statements (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id          UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    fiscal_date_ending  DATE NOT NULL,
    period_type         period_type NOT NULL,
    fiscal_year         INTEGER NOT NULL,
    fiscal_quarter      INTEGER,
    total_revenue       NUMERIC(20, 2),
    cost_of_revenue     NUMERIC(20, 2),
    gross_profit        NUMERIC(20, 2),
    operating_expense   NUMERIC(20, 2),
    operating_income    NUMERIC(20, 2),
    net_income          NUMERIC(20, 2),
    eps                 NUMERIC(10, 4),
    eps_diluted         NUMERIC(10, 4),
    interest_expense    NUMERIC(20, 2),
    income_tax_expense  NUMERIC(20, 2),
    ebitda              NUMERIC(20, 2),
    report_url          VARCHAR(500),
    filing_date         DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_income_stmt UNIQUE (company_id, fiscal_date_ending, period_type)
);

-- ============================================================
-- BALANCE_SHEETS
-- ============================================================
CREATE TABLE balance_sheets (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id              UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    fiscal_date_ending      DATE NOT NULL,
    period_type             period_type NOT NULL,
    fiscal_year             INTEGER NOT NULL,
    fiscal_quarter          INTEGER,
    total_assets            NUMERIC(20, 2),
    total_current_assets    NUMERIC(20, 2),
    cash_and_equivalents    NUMERIC(20, 2),
    total_liabilities       NUMERIC(20, 2),
    total_current_liabilities NUMERIC(20, 2),
    long_term_debt          NUMERIC(20, 2),
    total_debt              NUMERIC(20, 2),
    total_shareholder_equity NUMERIC(20, 2),
    retained_earnings       NUMERIC(20, 2),
    treasury_stock          NUMERIC(20, 2),
    shares_outstanding      BIGINT,
    report_url              VARCHAR(500),
    filing_date             DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_balance_sheet UNIQUE (company_id, fiscal_date_ending, period_type)
);

-- ============================================================
-- CASH_FLOW_STATEMENTS
-- ============================================================
CREATE TABLE cash_flow_statements (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id              UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    fiscal_date_ending      DATE NOT NULL,
    period_type             period_type NOT NULL,
    fiscal_year             INTEGER NOT NULL,
    fiscal_quarter          INTEGER,
    operating_cash_flow     NUMERIC(20, 2),
    capital_expenditure     NUMERIC(20, 2),
    free_cash_flow          NUMERIC(20, 2),
    cash_flow_investing     NUMERIC(20, 2),
    cash_flow_financing     NUMERIC(20, 2),
    dividends_paid          NUMERIC(20, 2),
    stock_issuance          NUMERIC(20, 2),
    debt_issuance           NUMERIC(20, 2),
    net_change_in_cash      NUMERIC(20, 2),
    report_url              VARCHAR(500),
    filing_date             DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_cash_flow UNIQUE (company_id, fiscal_date_ending, period_type)
);

-- ============================================================
-- FINANCIAL_RATIOS
-- ============================================================
CREATE TABLE financial_ratios (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id          UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    fiscal_date_ending  DATE NOT NULL,
    period_type         period_type NOT NULL,
    revenue_growth_yoy  NUMERIC(10, 4),
    revenue_growth_3y   NUMERIC(10, 4),
    revenue_growth_5y   NUMERIC(10, 4),
    eps_growth_yoy      NUMERIC(10, 4),
    fcf_growth_yoy      NUMERIC(10, 4),
    roe                 NUMERIC(10, 4),
    roa                 NUMERIC(10, 4),
    debt_to_equity      NUMERIC(10, 4),
    gross_margin        NUMERIC(10, 4),
    operating_margin    NUMERIC(10, 4),
    net_margin          NUMERIC(10, 4),
    pe_ratio            NUMERIC(10, 4),
    peg_ratio           NUMERIC(10, 4),
    dividend_yield      NUMERIC(10, 4),
    price_to_book       NUMERIC(10, 4),
    current_ratio       NUMERIC(10, 4),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_financial_ratio UNIQUE (company_id, fiscal_date_ending, period_type)
);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255),
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    role            user_role NOT NULL DEFAULT 'FREE',
    auth_provider   auth_provider NOT NULL DEFAULT 'LOCAL',
    google_id       VARCHAR(255),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_user_email UNIQUE (email)
);

-- ============================================================
-- WATCHLISTS
-- ============================================================
CREATE TABLE watchlists (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- WATCHLIST_STOCKS
-- ============================================================
CREATE TABLE watchlist_stocks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    watchlist_id    UUID NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sort_order      INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uq_watchlist_stock UNIQUE (watchlist_id, company_id)
);

-- ============================================================
-- ALERTS (Post-MVP)
-- ============================================================
CREATE TABLE alerts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id          UUID NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    alert_type          alert_type NOT NULL,
    threshold_value     NUMERIC(12, 4),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- SAVED_SCREENERS (Post-MVP)
-- ============================================================
CREATE TABLE saved_screeners (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    filters_json    JSONB NOT NULL,
    sort_field      VARCHAR(50),
    sort_direction  VARCHAR(4) DEFAULT 'DESC',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 4.3 Indexes

### Performance Indexes

```sql
-- ============================================================
-- COMPANY INDEXES
-- ============================================================
CREATE INDEX idx_company_ticker ON company (ticker);
CREATE INDEX idx_company_name ON company (name);
CREATE INDEX idx_company_sector ON company (sector);
CREATE INDEX idx_company_industry ON company (sector, industry);
CREATE INDEX idx_company_market_cap ON company (market_cap DESC);
CREATE INDEX idx_company_active ON company (is_active) WHERE is_active = TRUE;

-- Full-text search index
CREATE INDEX idx_company_search ON company
    USING GIN (to_tsvector('english', name || ' ' || COALESCE(description, '') || ' ' || ticker));

-- Trigram index for fuzzy search (autocomplete)
CREATE INDEX idx_company_ticker_trgm ON company USING GIN (ticker gin_trgm_ops);
CREATE INDEX idx_company_name_trgm ON company USING GIN (name gin_trgm_ops);

-- ============================================================
-- STOCK PRICES INDEXES (TimescaleDB auto-creates on date)
-- ============================================================
CREATE INDEX idx_prices_company_date ON stock_prices (company_id, date DESC);
CREATE INDEX idx_prices_date ON stock_prices (date DESC);

-- ============================================================
-- FINANCIAL STATEMENT INDEXES
-- ============================================================
-- Income Statements
CREATE INDEX idx_income_company_date ON income_statements (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_income_company_year ON income_statements (company_id, period_type, fiscal_year DESC);

-- Balance Sheets
CREATE INDEX idx_balance_company_date ON balance_sheets (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_balance_company_year ON balance_sheets (company_id, period_type, fiscal_year DESC);

-- Cash Flow
CREATE INDEX idx_cashflow_company_date ON cash_flow_statements (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_cashflow_company_year ON cash_flow_statements (company_id, period_type, fiscal_year DESC);

-- Financial Ratios
CREATE INDEX idx_ratios_company_date ON financial_ratios (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_ratios_company_year ON financial_ratios (company_id, period_type, fiscal_date_ending DESC);

-- ============================================================
-- WATCHLIST INDEXES
-- ============================================================
CREATE INDEX idx_watchlist_user ON watchlists (user_id);
CREATE INDEX idx_watchlist_stock_watchlist ON watchlist_stocks (watchlist_id);
CREATE INDEX idx_watchlist_stock_company ON watchlist_stocks (company_id);

-- ============================================================
-- USERS INDEXES
-- ============================================================
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_google_id ON users (google_id) WHERE google_id IS NOT NULL;

-- ============================================================
-- SCREENER PERFORMANCE INDEXES (Composite for common filter combos)
-- ============================================================
CREATE INDEX idx_screener_market_cap ON company (is_active, market_cap DESC)
    WHERE is_active = TRUE;
CREATE INDEX idx_screener_industry ON company (is_active, sector, industry)
    WHERE is_active = TRUE;
```

---

## 4.4 Constraints & Data Integrity

| Constraint | Type | Purpose |
|------------|------|---------|
| `uq_company_ticker` | UNIQUE | No duplicate tickers |
| `uq_company_cik` | UNIQUE | No duplicate SEC CIK numbers |
| `uq_income_stmt` | UNIQUE (company_id, fiscal_date_ending, period_type) | One statement per company per period |
| `uq_balance_sheet` | UNIQUE (company_id, fiscal_date_ending, period_type) | One balance sheet per period |
| `uq_cash_flow` | UNIQUE (company_id, fiscal_date_ending, period_type) | One cash flow per period |
| `uq_financial_ratio` | UNIQUE (company_id, fiscal_date_ending, period_type) | One ratio set per period |
| `uq_price_date` | UNIQUE (company_id, date) | One price record per company per day |
| `uq_watchlist_stock` | UNIQUE (watchlist_id, company_id) | Prevent duplicate stocks in watchlist |
| `uq_user_email` | UNIQUE | No duplicate emails |
| `uq_industry` | UNIQUE (sector, industry_group, industry) | No duplicate industry classifications |
| All FKs | FOREIGN KEY ... ON DELETE CASCADE | Cascade deletes for related data |

---

## 4.5 Partitioning Strategy

### TimescaleDB Hypertable (stock_prices)

```sql
-- stock_prices uses TimescaleDB automatic chunking
-- Chunk interval: 1 month
-- ~6,000 rows per company × 10 years × 252 trading days ≈ 15M rows for S&P 500

SELECT create_hypertable('stock_prices', 'date',
    chunk_time_interval => INTERVAL '1 month');

-- Enable compression for chunks older than 30 days
ALTER TABLE stock_prices SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'company_id',
    timescaledb.compress_orderby = 'date DESC'
);

SELECT add_compression_policy('stock_prices', INTERVAL '30 days');
```

### Why not partition financial statements?

Financial statements are much smaller:
- S&P 500 × 40 quarters × 3 statement types = 60,000 rows
- Not enough volume to justify partitioning
- Standard B-tree indexes are sufficient

---

## 4.6 Materialized Views

### Screener Materialized View

```sql
CREATE MATERIALIZED VIEW mv_screener_data AS
SELECT
    c.id AS company_id,
    c.ticker,
    c.name,
    c.sector,
    c.industry,
    c.market_cap,
    fr.revenue_growth_yoy,
    fr.eps_growth_yoy,
    fr.roe,
    fr.roa,
    fr.debt_to_equity,
    fr.gross_margin,
    fr.operating_margin,
    fr.net_margin,
    fr.pe_ratio,
    fr.peg_ratio,
    fr.dividend_yield,
    fr.price_to_book,
    fr.current_ratio,
    fr.fiscal_date_ending
FROM company c
JOIN LATERAL (
    SELECT *
    FROM financial_ratios
    WHERE company_id = c.id
      AND period_type = 'ANNUAL'
    ORDER BY fiscal_date_ending DESC
    LIMIT 1
) fr ON TRUE
WHERE c.is_active = TRUE;

CREATE UNIQUE INDEX idx_mv_screener_company ON mv_screener_data (company_id);
CREATE INDEX idx_mv_screener_sector ON mv_screener_data (sector);
CREATE INDEX idx_mv_screener_pe ON mv_screener_data (pe_ratio);
CREATE INDEX idx_mv_screener_market_cap ON mv_screener_data (market_cap DESC);
CREATE INDEX idx_mv_screener_rev_growth ON mv_screener_data (revenue_growth_yoy DESC);
CREATE INDEX idx_mv_screener_roe ON mv_screener_data (roe DESC);
```

### Industry Averages Materialized View

```sql
CREATE MATERIALIZED VIEW mv_industry_averages AS
SELECT
    c.sector,
    c.industry,
    COUNT(*) AS company_count,
    AVG(c.market_cap) AS avg_market_cap,
    AVG(fr.pe_ratio) AS avg_pe,
    AVG(fr.revenue_growth_yoy) AS avg_revenue_growth,
    AVG(fr.roe) AS avg_roe,
    AVG(fr.debt_to_equity) AS avg_debt_to_equity,
    AVG(fr.net_margin) AS avg_net_margin,
    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY fr.pe_ratio) AS pe_25th,
    PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY fr.pe_ratio) AS pe_median,
    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY fr.pe_ratio) AS pe_75th
FROM company c
JOIN LATERAL (
    SELECT *
    FROM financial_ratios
    WHERE company_id = c.id AND period_type = 'ANNUAL'
    ORDER BY fiscal_date_ending DESC
    LIMIT 1
) fr ON TRUE
WHERE c.is_active = TRUE
GROUP BY c.sector, c.industry;

CREATE UNIQUE INDEX idx_mv_industry ON mv_industry_averages (sector, industry);
```

### Refresh Strategy

```sql
-- Called at end of nightly ETL
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_screener_data;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_industry_averages;
```

---

## 4.7 Estimated Data Volumes

| Table | S&P 500 | Russell 3000 | All US (~6K) |
|-------|---------|-------------|-------------|
| `company` | 500 | 3,000 | 6,000 |
| `stock_prices` (10yr) | ~1.3M | ~7.6M | ~15M |
| `income_statements` (10yr) | ~20K | ~120K | ~240K |
| `balance_sheets` (10yr) | ~20K | ~120K | ~240K |
| `cash_flow_statements` (10yr) | ~20K | ~120K | ~240K |
| `financial_ratios` (10yr) | ~20K | ~120K | ~240K |
| `users` | — | — | 500 (target) |
| `watchlists` | — | — | ~1,000 |
| **Total** | ~1.4M | ~8.1M | ~16M |

PostgreSQL handles 16M rows easily on modest hardware. The only table that grows continuously is `stock_prices`, which TimescaleDB handles with compression and chunking.
