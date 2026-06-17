# Section 14: Dashboard UI Design

## 14.1 Dashboard Layout

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  [Logo] StockHub    [Search Bar: "Search ticker or company..."]  [Login] 👤  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ COMPANY HEADER                                                          │ │
│  │ ┌──────┐                                                                │ │
│  │ │ LOGO │  AAPL · Apple Inc.                          ⭐ Add to Watchlist │ │
│  │ │      │  Technology · Consumer Electronics                             │ │
│  │ └──────┘  $178.50  ▲ +$3.20 (+1.8%)  ·  NasdaqGS · Delayed 15 min      │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌───────────────────────────────────┐  ┌──────────────────────────────────┐ │
│  │         PRICE CHART (1Y)          │  │       KEY METRICS                │ │
│  │                                   │  │                                  │ │
│  │   [1M] [3M] [6M] [1Y] [5Y] [MAX] │  │  Market Cap     $2.80T           │ │
│  │                                   │  │  P/E Ratio       28.5            │ │
│  │         /\    /\                  │  │  Revenue Growth   5.2%           │ │
│  │        /  \  /  \    /\          │  │  ROE            145.3%           │ │
│  │   ___/    \/    \__/  \____     │  │  Net Margin      26.4%           │ │
│  │  /                        \      │  │  Dividend Yield   0.52%          │ │
│  │ /                          \     │  │  Debt/Equity      1.95           │ │
│  │────────────────────────────────│  │  │  52W Range   $124-$199          │ │
│  │       VOLUME BARS              │  │  │  Avg Volume    55.2M            │ │
│  └───────────────────────────────────┘  └──────────────────────────────────┘ │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ [Overview] [Financials] [Metrics] [Peers]                               │ │
│  ├─────────────────────────────────────────────────────────────────────────┤ │
│  │                                                                         │ │
│  │  TAB: Financials                                                        │ │
│  │  [Annual ▾]  [Income Statement ▾]  [5 years ▾]                         │ │
│  │                                                                         │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │ │
│  │  │                    INCOME STATEMENT (Annual)                     │   │ │
│  │  │  ($ in Millions)                                                 │   │ │
│  │  │                                                                  │   │ │
│  │  │                    FY2024    FY2023    FY2022    FY2021    FY2020 │   │ │
│  │  │  ─────────────────────────────────────────────────────────────  │   │ │
│  │  │  Total Revenue    383,285   394,328   365,817   347,155   274,515│   │ │
│  │  │  Gross Profit     169,148   170,782   155,635   152,836   104,956│   │ │
│  │  │  Operating Income 114,301   119,437   114,573   108,949    66,288│   │ │
│  │  │  Net Income        96,995    99,803    94,680    90,614    57,411│   │ │
│  │  │  EPS (Diluted)       6.11      6.13      5.89      5.61      3.28│   │ │
│  │  │  ─────────────────────────────────────────────────────────────  │   │ │
│  │  │  Gross Margin       44.1%     43.3%     42.5%     44.0%     38.2%│   │ │
│  │  │  Operating Margin   29.8%     30.3%     31.3%     31.4%     24.1%│   │ │
│  │  │  Net Margin         25.3%     25.3%     25.9%     26.1%     20.9%│   │ │
│  │  └─────────────────────────────────────────────────────────────────┘   │ │
│  │                                                                         │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │ │
│  │  │                  REVENUE & NET INCOME TREND                      │   │ │
│  │  │                                                                  │   │ │
│  │  │   $400B ┤                                                        │   │ │
│  │  │   $350B ┤    ┌────┐────┐────┐────┐                               │   │ │
│  │  │   $300B ┤    │    │    │    │    │  ████ Revenue                 │   │ │
│  │  │   $250B ┤    │    │    │    │    │  ──── Net Income             │   │ │
│  │  │   $200B ┤    │    │    │    │    │                               │   │ │
│  │  │         └────┴────┴────┴────┴────┘                               │   │ │
│  │  │         FY20  FY21  FY22  FY23  FY24                             │   │ │
│  │  └─────────────────────────────────────────────────────────────────┘   │ │
│  │                                                                         │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ PEER COMPARISON                    [Compare in detail →]                │ │
│  │                                                                         │ │
│  │  Metric        AAPL      MSFT     GOOGL     Industry Avg               │ │
│  │  ─────────────────────────────────────────────────────                  │ │
│  │  P/E            28.5      35.2     22.1         25.1                   │ │
│  │  Rev Growth      5.2%     15.2%    13.4%        12.3%                  │ │
│  │  ROE           145.3%     42.1%    27.3%        35.2%                  │ │
│  │  Net Margin     26.4%     35.4%    24.0%        18.7%                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │ ABOUT APPLE INC.                                                        │ │
│  │ Apple Inc. designs, manufactures, and markets smartphones, personal      │ │
│  │ computers, tablets, wearables, and accessories worldwide...              │ │
│  │ Founded: 1976 · HQ: Cupertino, CA · Employees: 161,000                  │ │
│  │ Website: www.apple.com                                                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 14.2 Component Tree

```
CompanyDashboardComponent
├── CompanyHeaderComponent
│   ├── Company logo (image)
│   ├── Ticker + Name display
│   ├── Price display (color-coded green/red)
│   ├── Price change (absolute + percentage)
│   └── Add-to-Watchlist button (⭐ toggle)
│
├── PriceChartComponent
│   ├── Chart.js line chart (adjusted close)
│   ├── Range selector: [1M] [3M] [6M] [1Y] [5Y] [MAX]
│   └── Volume bar chart overlay
│
├── MetricsGridComponent
│   ├── MetricCardComponent × 8
│   │   ├── Label (e.g., "Market Cap")
│   │   ├── Value (formatted: $2.80T)
│   │   ├── Optional: context (e.g., "+3.5% YoY")
│   │   └── Tooltip with definition on hover
│   └── MetricCardComponent configurations defined in parent
│
├── TabGroupComponent
│   ├── Tab: Overview
│   │   ├── Company description
│   │   ├── Key facts (founded, HQ, employees, website)
│   │   └── Sector/Industry classification
│   │
│   ├── Tab: Financials
│   │   ├── Period selector: Annual / Quarterly
│   │   ├── Statement selector: Income / Balance Sheet / Cash Flow
│   │   ├── Year range selector: 3Y / 5Y / 10Y
│   │   ├── FinancialTableComponent (sortable columns)
│   │   └── RevenueTrendChartComponent (bar + line combo)
│   │
│   ├── Tab: Metrics
│   │   ├── GrowthMetricsChart (bar chart: 1Y/3Y/5Y revenue, EPS, FCF)
│   │   ├── ProfitabilityMetricsChart (ROE, ROA, Margins trend)
│   │   └── ValuationMetricsChart (P/E, P/B, P/S trend)
│   │
│   └── Tab: Peers
│       ├── PeerComparisonTable (5 peers + industry avg)
│       └── "Compare in Detail" link → navigates to /compare
│
└── PeerComparisonWidgetComponent (below tabs, always visible)
    ├── Comparison table (4-5 key metrics vs peers)
    └── Link to full comparison page
```

---

## 14.3 UX Details

### Search Bar (Global)
- **Position**: Fixed in top navbar, always accessible
- **Behavior**: As user types (debounce 300ms), autocomplete dropdown appears below
- **Autocomplete items**: Ticker + company name + sector badge
- **Keyboard navigation**: Arrow keys + Enter to select
- **Empty state**: "Search by ticker (AAPL) or company name (Apple)"
- **No results**: "No companies found matching '{query}'"

### Price Chart
- **Default range**: 1 Year
- **Interaction**: Hover shows crosshair with date/open/high/low/close/volume
- **Range buttons**: Instant redraw without page reload
- **Color**: Candlestick or line with gradient fill below
- **Loading state**: Skeleton chart with pulse animation
- **Error state**: "Unable to load price data. Try again."

### Metric Cards
- **Layout**: CSS Grid, 4 columns on desktop, 2 on tablet
- **Hover**: Tooltip with plain-English definition
  - Example: "Return on Equity (ROE) measures how efficiently a company uses shareholder money to generate profits. Higher is generally better."
- **Color coding**: Green = positive/favorable, Red = negative/unfavorable, Gray = neutral
- **Thresholds for color**:
  - ROE > 15% = green, ROE < 5% = red
  - Revenue growth > 10% = green, < 0% = red
  - D/E < 1.0 = green, > 3.0 = red

### Financial Statement Tables
- **Default**: Annual data, 5 years
- **Sticky header row**: Year columns remain visible while scrolling
- **Horizontal scroll**: For 10-year view
- **Calculated rows** (margins, growth rates) visually distinct (italic, lighter background)
- **Quarterly toggle**: Switches to quarterly view with fiscal quarter labels (Q1 FY2024, Q2 FY2024...)
- **Export button** (Premium): "Download Excel" / "Download PDF"

### Peer Comparison Widget
- **Default peers**: Top 4 companies in same industry by market cap
- **Metrics shown** (configurable): P/E, Revenue Growth, ROE, Net Margin
- **Visual**: Green/red shading relative to peer group
- **Industry average row**: Highlighted (bold, different background)
- **"Compare in Detail"**: Opens full comparison page with all 3-5 selected peers

### Loading States
- **Initial load**: Skeleton screen matching layout (gray rectangles in component shapes)
- **Tab switch**: Skeleton inside tab content area (not full page)
- **Chart loading**: Pulsing gray area with chart dimensions
- **Data table loading**: 5 rows × 5 columns skeleton

### Error States
- **Company not found**: "We couldn't find '{ticker}'. It may not be in the S&P 500 yet. [Search for another stock]"
- **API error**: Toast notification: "Unable to load data. Retrying..." with retry button
- **Network offline**: Banner at top: "You're offline. Data may not be current."

---

## 14.4 Responsive Breakpoints

| Breakpoint | Width | Layout Changes |
|-----------|-------|---------------|
| Desktop | > 1200px | Full 3-column: chart + metrics grid + peer widget side by side |
| Laptop | 900-1200px | 2-column: chart full width, metrics + peer stacked below |
| Tablet | 600-900px | Single column: all sections stacked; financial table horizontal scroll |
| Mobile | < 600px | Single column; simplified charts; metric cards 2-column; tab labels as icons |
