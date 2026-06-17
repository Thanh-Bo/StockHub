# Section 1: Product Architecture

## 1.1 Target Users

| User Type | Description | % of Users |
|-----------|-------------|------------|
| **Retail Investors** | Individual investors researching stocks, managing portfolios, seeking fundamental analysis | 80% |
| **Finance Students** | Learning valuation, comparing companies, understanding financial metrics | 15% |
| **Early-Career Analysts** | Quick company lookups, peer comparisons, data exports | 5% |

**Primary persona**: A retail investor in their 20s-40s who wants clean, fast access to company fundamentals without paying for Bloomberg Terminal or FactSet. They understand basic financial concepts (P/E ratio, revenue growth) but want help interpreting data.

---

## 1.2 User Journeys

### Journey 1: Quick Company Research
```
Open StockHub → Search "AAPL" → See dashboard with price chart + key metrics 
→ Click "Financials" tab → Browse income statement → Compare to MSFT 
→ Add AAPL to watchlist → Leave
```
**Time target**: Under 30 seconds to reach the dashboard.

### Journey 2: Stock Discovery
```
Open StockHub → Open Screener → Filter: 
  Market Cap > $10B, P/E < 25, Revenue Growth > 15%, Sector = Technology
→ See 23 matching stocks → Sort by ROE descending → Click into top result 
→ Read company profile → Add to watchlist
```

### Journey 3: Portfolio Monitoring
```
Login → See watchlist dashboard → Notice NVDA price crossed a threshold 
→ Click into NVDA dashboard → Review latest quarter financials 
→ Check growth trends (5-year chart) → Decide to hold → Close
```

### Journey 4: Deep Analysis
```
Search "JPM" → Dashboard → Financial Statements → Income Statement (annual, 10 years) 
→ Switch to Balance Sheet → Compare JPM vs BAC vs WFC 
→ Export comparison to Excel → Download for offline analysis
```

---

## 1.3 Main Features

### MVP Features (Build First — 3-4 months)

| # | Feature | Description | Priority |
|---|---------|-------------|----------|
| 1 | **Stock Search** | Search by ticker or company name with autocomplete | P0 |
| 2 | **Company Dashboard** | Overview page with price chart, metrics cards, company profile | P0 |
| 3 | **Financial Statements** | Income Statement, Balance Sheet, Cash Flow (annual + quarterly) | P0 |
| 4 | **Stock Screener** | Filter stocks by 8+ financial criteria | P1 |
| 5 | **Company Comparison** | Side-by-side comparison of 2-5 companies | P1 |
| 6 | **Watchlist** | Create/manage watchlists of stocks to track | P1 |
| 7 | **Authentication** | Register, login, Google OAuth, JWT | P0 |
| 8 | **Historical Price Charts** | Interactive 1Y/5Y/MAX price chart with volume | P0 |
| 9 | **Financial Ratios** | Auto-calculated: P/E, ROE, ROA, Debt/Equity, Margins, PEG | P1 |
| 10 | **Growth Trends** | Revenue growth, EPS growth, FCF growth over 1/3/5/10 years | P2 |

### Post-MVP Features

| # | Feature | Priority |
|---|---------|----------|
| 11 | Email Alerts (price thresholds, earnings dates) | P2 |
| 12 | Premium Plans (role-based feature gating) | P3 |
| 13 | Saved Screeners (persist screener configurations) | P2 |
| 14 | Excel/PDF Export | P2 |
| 15 | Industry Averages (sector/industry benchmarks) | P2 |
| 16 | Educational Content (metric explanations, tooltips) | P3 |
| 17 | API Access (developer API for premium users) | P3 |
| 18 | Mobile PWA (responsive enhancement) | P3 |

---

## 1.4 Monetization Strategy

| Tier | Features | Price |
|------|----------|-------|
| **FREE** | Dashboard, Search, Basic Financials, 1 Watchlist (max 10 stocks), Basic Screener (5 filters), Price Charts | $0 |
| **PREMIUM** | Advanced Screener (all filters, save configurations), Unlimited Watchlists, Company Comparison, Exports (Excel/PDF), Industry Averages, Growth Trends (10-year), No Ads | $9.99/mo or $79.99/yr |
| **ADMIN** | All features + user management, data ingestion controls | Internal |

**Implementation note**: No Stripe/payment integration initially. Premium is role-based — manually assignable in the database. Payment integration is a post-MVP enhancement.

---

## 1.5 MVP Scope

```
MVP = Features that demonstrate backend engineering depth
      without requiring payment infrastructure.

MVP CUT LINE:
✅ Stock Search        ✅ Company Dashboard      ✅ Financial Statements
✅ Stock Screener      ✅ Company Comparison     ✅ Watchlist
✅ Authentication      ✅ Price Charts           ✅ Financial Ratios
✅ Growth Trends
───────────────────────────────────────────────────────────
❌ Email Alerts        ❌ Premium Plans (UI gating only)
❌ Saved Screeners     ❌ Excel/PDF Export
❌ Industry Averages   ❌ Educational Content
❌ API Access          ❌ Mobile PWA
```

**MVP Success Criteria**:
- User can search, view dashboard, and read financial statements for any S&P 500 stock
- User can filter S&P 500 stocks using 8 screening criteria
- User can compare 2-3 companies side-by-side
- User can create a watchlist and see price movements
- Dashboard loads in under 500ms
- 10 years of historical data available
- System handles 50 concurrent users

---

## 1.6 Future Roadmap

```
PHASE 1 (Months 1-3):         PHASE 2 (Months 4-6):         PHASE 3 (Months 7-9):
├── Project setup              ├── Excel/PDF Export          ├── Email Alerts
├── Auth system                ├── Industry Averages         ├── Premium gating (UI)
├── Company + Price data       ├── Saved Screeners           ├── Educational tooltips
├── Financial statements       ├── Advanced Screener         ├── Russell 3000 universe
├── Dashboard UI               ├── Growth trend charts       ├── PWA optimization
├── Search                     ├── Performance tuning        ├── API access layer
├── Basic Screener             ├── Redis caching             └── CI/CD hardening
├── Watchlist                  └── Testing suite
└── Company Comparison

PHASE 4 (Months 10-12):        BEYOND:
├── Payment integration        ├── Mobile app (React Native or Flutter)
├── All US stocks              ├── Real-time WebSocket prices
├── Portfolio tracking         ├── AI-powered stock analysis
├── News feed integration      ├── Social features (shared watchlists)
└── Admin dashboard            └── Backtesting engine
```
