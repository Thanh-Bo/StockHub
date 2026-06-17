# Section 15: Watchlist System

## 15.1 Data Model (Recap)

```
User (1) ────< (N) Watchlist (1) ────< (N) WatchlistStock (N) >──── (1) Company
```

A user has multiple watchlists. Each watchlist contains multiple stocks. Each stock can appear in multiple watchlists.

---

## 15.2 Watchlist Features

### MVP
- Create watchlist (name, description)
- Add stocks by ticker
- Remove stocks
- Reorder stocks (drag-and-drop)
- View watchlist with current prices
- Default watchlist created on registration

### Post-MVP
- Unlimited watchlists (Premium)
- Price alerts on watchlist stocks
- Email notifications for alerts

---

## 15.3 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/watchlists` | List user's watchlists |
| `POST` | `/api/v1/watchlists` | Create watchlist |
| `GET` | `/api/v1/watchlists/{id}` | Get watchlist with stock summaries |
| `PUT` | `/api/v1/watchlists/{id}` | Update name/description |
| `DELETE` | `/api/v1/watchlists/{id}` | Delete watchlist |
| `POST` | `/api/v1/watchlists/{id}/stocks` | Add stock |
| `DELETE` | `/api/v1/watchlists/{id}/stocks/{ticker}` | Remove stock |
| `PUT` | `/api/v1/watchlists/{id}/stocks/reorder` | Reorder stocks |

---

## 15.4 Backend Implementation

### Service Layer

```java
package com.stockhub.watchlist;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository watchlistRepo;
    private final WatchlistStockRepository watchlistStockRepo;
    private final CompanyRepository companyRepo;
    private final PriceRepository priceRepo;
    private final RedisTemplate<String, Object> redisTemplate;

    // Get watchlist with real-time price summaries
    @Cacheable(value = "watchlist", key = "#watchlistId")
    public WatchlistDetailResponse getWatchlistDetail(UUID watchlistId, UUID userId) {
        Watchlist watchlist = watchlistRepo.findByIdAndUserId(watchlistId, userId)
            .orElseThrow(() -> new WatchlistNotFoundException(watchlistId));

        List<WatchlistStock> stocks = watchlistStockRepo
            .findByWatchlistIdOrderBySortOrder(watchlistId);

        // Batch-fetch latest prices
        List<UUID> companyIds = stocks.stream()
            .map(WatchlistStock::getCompanyId)
            .toList();
        Map<UUID, StockPrice> prices = priceRepo.findLatestPrices(companyIds);

        // Assemble response
        List<WatchlistStockSummary> summaries = stocks.stream()
            .map(ws -> buildSummary(ws, prices.get(ws.getCompanyId())))
            .toList();

        return new WatchlistDetailResponse(
            watchlist.getId(),
            watchlist.getName(),
            watchlist.getDescription(),
            summaries
        );
    }

    // Add stock — enforces tier limits
    public void addStock(UUID watchlistId, UUID userId, String ticker) {
        Watchlist watchlist = watchlistRepo.findByIdAndUserId(watchlistId, userId)
            .orElseThrow(() -> new WatchlistNotFoundException(watchlistId));

        Company company = companyRepo.findByTicker(ticker)
            .orElseThrow(() -> new CompanyNotFoundException(ticker));

        // Check tier limits
        User user = userRepo.findById(userId).orElseThrow();
        long currentCount = watchlistStockRepo.countByWatchlistId(watchlistId);
        if (user.getRole() == UserRole.FREE && currentCount >= 10) {
            throw new WatchlistLimitExceededException(
                "Free users can have up to 10 stocks per watchlist. Upgrade to Premium for unlimited.");
        }

        // Check duplicate
        if (watchlistStockRepo.existsByWatchlistIdAndCompanyId(watchlistId, company.getId())) {
            throw new DuplicateStockException(ticker + " is already in this watchlist");
        }

        int nextOrder = watchlistStockRepo.getMaxSortOrder(watchlistId) + 1;
        WatchlistStock ws = WatchlistStock.builder()
            .watchlistId(watchlistId)
            .companyId(company.getId())
            .sortOrder(nextOrder)
            .build();
        watchlistStockRepo.save(ws);

        // Evict cache
        evictWatchlistCache(watchlistId);
    }

    // Check limits when creating watchlist
    public WatchlistResponse createWatchlist(UUID userId, CreateWatchlistRequest request) {
        User user = userRepo.findById(userId).orElseThrow();
        long watchlistCount = watchlistRepo.countByUserId(userId);

        if (user.getRole() == UserRole.FREE && watchlistCount >= 1) {
            throw new WatchlistLimitExceededException(
                "Free users can have 1 watchlist. Upgrade to Premium for unlimited.");
        }

        Watchlist watchlist = Watchlist.builder()
            .userId(userId)
            .name(request.name())
            .description(request.description())
            .isDefault(watchlistCount == 0)
            .sortOrder((int) watchlistCount)
            .build();

        return toResponse(watchlistRepo.save(watchlist));
    }
}
```

---

## 15.5 Watchlist Price Summary (Optimized Query)

Instead of fetching prices one-by-one, batch load with a single query:

```java
@Repository
public interface PriceRepository extends JpaRepository<StockPrice, UUID> {

    // Get latest price for multiple companies in one query
    @Query(value = """
        SELECT DISTINCT ON (sp.company_id)
            sp.company_id, sp.close, sp.adjusted_close, sp.volume,
            sp.date, prev.close AS previous_close
        FROM stock_prices sp
        LEFT JOIN LATERAL (
            SELECT close FROM stock_prices
            WHERE company_id = sp.company_id AND date < sp.date
            ORDER BY date DESC LIMIT 1
        ) prev ON TRUE
        WHERE sp.company_id IN (:companyIds)
        ORDER BY sp.company_id, sp.date DESC
        """, nativeQuery = true)
    List<PriceSummaryProjection> findLatestPrices(@Param("companyIds") List<UUID> companyIds);
}
```

**Price Summary Response**:
```json
{
  "ticker": "AAPL",
  "name": "Apple Inc.",
  "latestPrice": 178.50,
  "priceChange": 3.20,
  "priceChangePercent": 1.83,
  "marketCap": 2800000000000,
  "addedAt": "2026-01-15T14:30:00Z"
}
```

---

## 15.6 Frontend Implementation

### Watchlist Store

```typescript
// watchlist.store.ts
export const WatchlistStore = signalStore(
  withState({
    watchlists: [] as WatchlistSummary[],
    selectedWatchlist: null as WatchlistDetail | null,
    loading: false,
  }),

  withMethods((store, watchlistService = inject(WatchlistService)) => ({
    async loadWatchlists() {
      patchState(store, { loading: true });
      const watchlists = await firstValueFrom(watchlistService.getWatchlists());
      patchState(store, { watchlists, loading: false });
    },

    async selectWatchlist(id: string) {
      patchState(store, { loading: true });
      const detail = await firstValueFrom(watchlistService.getWatchlistDetail(id));
      patchState(store, { selectedWatchlist: detail, loading: false });
    },

    async addStock(watchlistId: string, ticker: string) {
      await firstValueFrom(watchlistService.addStock(watchlistId, ticker));
      // Refresh the selected watchlist
      await this.selectWatchlist(watchlistId);
    },

    async removeStock(watchlistId: string, ticker: string) {
      await firstValueFrom(watchlistService.removeStock(watchlistId, ticker));
      await this.selectWatchlist(watchlistId);
    },

    async reorderStocks(watchlistId: string, orderedTickers: string[]) {
      await firstValueFrom(watchlistService.reorderStocks(watchlistId, orderedTickers));
      await this.selectWatchlist(watchlistId);
    },
  }))
);
```

### Drag-and-Drop Reorder (Angular Material CDK)

```typescript
// watchlist-detail.component.ts
@Component({ /* ... */ })
export class WatchlistDetailComponent {
  store = inject(WatchlistStore);

  drop(event: CdkDragDrop<string[]>) {
    const stocks = [...this.store.selectedWatchlist()!.stocks];
    moveItemInArray(stocks, event.previousIndex, event.currentIndex);
    this.store.reorderStocks(
      this.store.selectedWatchlist()!.id,
      stocks.map(s => s.ticker)
    );
  }
}
```

---

## 15.7 Alert System (Post-MVP Design)

### Alert Types

| Type | Trigger | Example |
|------|---------|---------|
| `PRICE_ABOVE` | Stock price crosses above threshold | NVDA > $500 |
| `PRICE_BELOW` | Stock price falls below threshold | AAPL < $150 |
| `EARNINGS_DATE` | Company files quarterly earnings | Upcoming earnings date |

### Alert Processing

```java
// Runs after PriceIngestionJob (6:30 PM ET)
@Component
public class AlertProcessor {

    @Scheduled(cron = "0 30 18 * * ?", zone = "America/New_York")
    public void processPriceAlerts() {
        List<Alert> activeAlerts = alertRepo.findByIsActiveTrue();

        for (Alert alert : activeAlerts) {
            BigDecimal currentPrice = priceRepo.findLatestClose(alert.getCompanyId())
                .orElse(null);
            if (currentPrice == null) continue;

            boolean triggered = switch (alert.getAlertType()) {
                case PRICE_ABOVE ->
                    currentPrice.compareTo(alert.getThresholdValue()) >= 0;
                case PRICE_BELOW ->
                    currentPrice.compareTo(alert.getThresholdValue()) <= 0;
                case EARNINGS_DATE -> false; // Handled separately
            };

            if (triggered) {
                sendAlert(alert, currentPrice);
                alertRepo.updateLastTriggered(alert.getId(), Instant.now());
            }
        }
    }

    private void sendAlert(Alert alert, BigDecimal currentPrice) {
        User user = userRepo.findById(alert.getUserId()).orElseThrow();
        Company company = companyRepo.findById(alert.getCompanyId()).orElseThrow();

        // Email notification (via EmailService abstraction)
        emailService.sendAlertEmail(
            user.getEmail(),
            company.getTicker(),
            alert.getAlertType(),
            alert.getThresholdValue(),
            currentPrice
        );

        // In-app notification (stored in Redis for polling or WebSocket)
        redisTemplate.opsForList().leftPush(
            "notifications:" + alert.getUserId(),
            new AlertNotification(company.getTicker(), alert.getAlertType(), currentPrice)
        );
    }
}
```

### EmailService Abstraction

```java
public interface EmailService {
    void sendAlertEmail(String to, String ticker, AlertType type,
                        BigDecimal threshold, BigDecimal currentPrice);
    void sendVerificationEmail(String to, String token);
    void sendWelcomeEmail(String to, String firstName);
}

// Post-MVP implementation: Resend or SendGrid
@Component
public class ResendEmailService implements EmailService { /* ... */ }
```
