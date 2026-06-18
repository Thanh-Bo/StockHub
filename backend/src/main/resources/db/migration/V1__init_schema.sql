-- ============================================================
-- StockHub V1: Base Schema
-- PostgreSQL 16 + TimescaleDB + pg_trgm
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================================
-- ENUMS
-- ============================================================
CREATE TYPE user_role AS ENUM ('FREE', 'PREMIUM', 'ADMIN');
CREATE TYPE auth_provider AS ENUM ('LOCAL', 'GOOGLE');
CREATE TYPE period_type AS ENUM ('ANNUAL', 'QUARTERLY');
CREATE TYPE alert_type AS ENUM ('PRICE_ABOVE', 'PRICE_BELOW', 'EARNINGS_DATE');

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
    search_vector   tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', COALESCE(name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(ticker, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(sector, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(industry, '')), 'C')
    ) STORED,
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
-- ALERTS (Post-MVP table, created now for schema completeness)
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

-- ============================================================
-- VALIDATION_ERRORS (ETL monitoring)
-- ============================================================
CREATE TABLE validation_errors (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_name        VARCHAR(100) NOT NULL,
    ticker          VARCHAR(10),
    provider        VARCHAR(50),
    error_message   TEXT NOT NULL,
    raw_data_json   JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Company indexes
CREATE INDEX idx_company_ticker ON company (ticker);
CREATE INDEX idx_company_name ON company (name);
CREATE INDEX idx_company_sector ON company (sector);
CREATE INDEX idx_company_industry ON company (sector, industry);
CREATE INDEX idx_company_market_cap ON company (market_cap DESC);
CREATE INDEX idx_company_active ON company (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_company_search_vector ON company USING GIN (search_vector);
CREATE INDEX idx_company_ticker_trgm ON company USING GIN (ticker gin_trgm_ops);
CREATE INDEX idx_company_name_trgm ON company USING GIN (name gin_trgm_ops);

-- Stock price indexes
CREATE INDEX idx_prices_company_date ON stock_prices (company_id, date DESC);
CREATE INDEX idx_prices_date ON stock_prices (date DESC);

-- Financial statement indexes
CREATE INDEX idx_income_company_date ON income_statements (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_income_company_year ON income_statements (company_id, period_type, fiscal_year DESC);
CREATE INDEX idx_balance_company_date ON balance_sheets (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_balance_company_year ON balance_sheets (company_id, period_type, fiscal_year DESC);
CREATE INDEX idx_cashflow_company_date ON cash_flow_statements (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_cashflow_company_year ON cash_flow_statements (company_id, period_type, fiscal_year DESC);
CREATE INDEX idx_ratios_company_date ON financial_ratios (company_id, fiscal_date_ending DESC);
CREATE INDEX idx_ratios_company_year ON financial_ratios (company_id, period_type, fiscal_date_ending DESC);

-- Watchlist indexes
CREATE INDEX idx_watchlist_user ON watchlists (user_id);
CREATE INDEX idx_watchlist_stock_watchlist ON watchlist_stocks (watchlist_id);
CREATE INDEX idx_watchlist_stock_company ON watchlist_stocks (company_id);

-- User indexes
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_google_id ON users (google_id) WHERE google_id IS NOT NULL;

-- Screener performance indexes
CREATE INDEX idx_screener_market_cap ON company (is_active, market_cap DESC) WHERE is_active = TRUE;
CREATE INDEX idx_screener_industry ON company (is_active, sector, industry) WHERE is_active = TRUE;

-- Validation errors index
CREATE INDEX idx_val_errors_job ON validation_errors (job_name, created_at DESC);

-- ============================================================
-- MATERIALIZED VIEWS
-- ============================================================

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
    WHERE company_id = c.id AND period_type = 'ANNUAL'
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
    SELECT * FROM financial_ratios
    WHERE company_id = c.id AND period_type = 'ANNUAL'
    ORDER BY fiscal_date_ending DESC LIMIT 1
) fr ON TRUE
WHERE c.is_active = TRUE
GROUP BY c.sector, c.industry;

CREATE UNIQUE INDEX idx_mv_industry ON mv_industry_averages (sector, industry);
