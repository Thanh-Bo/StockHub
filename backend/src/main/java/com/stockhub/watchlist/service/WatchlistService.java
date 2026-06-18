package com.stockhub.watchlist.service;

import com.stockhub.auth.entity.User;
import com.stockhub.auth.repository.UserRepository;
import com.stockhub.common.exception.CompanyNotFoundException;
import com.stockhub.common.exception.DuplicateStockException;
import com.stockhub.common.exception.ResourceNotFoundException;
import com.stockhub.common.exception.WatchlistLimitExceededException;
import com.stockhub.common.exception.WatchlistNotFoundException;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.prices.entity.StockPrice;
import com.stockhub.prices.repository.StockPriceRepository;
import com.stockhub.watchlist.dto.CreateWatchlistRequest;
import com.stockhub.watchlist.dto.UpdateWatchlistRequest;
import com.stockhub.watchlist.dto.WatchlistDetailResponse;
import com.stockhub.watchlist.dto.WatchlistResponse;
import com.stockhub.watchlist.dto.WatchlistStockSummary;
import com.stockhub.watchlist.dto.WatchlistSummaryResponse;
import com.stockhub.watchlist.entity.Watchlist;
import com.stockhub.watchlist.entity.WatchlistStock;
import com.stockhub.watchlist.repository.WatchlistRepository;
import com.stockhub.watchlist.repository.WatchlistStockRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistService {

    private static final int FREE_WATCHLIST_LIMIT = 1;
    private static final int FREE_STOCK_LIMIT = 10;

    private final WatchlistRepository watchlistRepository;
    private final WatchlistStockRepository watchlistStockRepository;
    private final CompanyRepository companyRepository;
    private final StockPriceRepository stockPriceRepository;
    private final UserRepository userRepository;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            WatchlistStockRepository watchlistStockRepository,
                            CompanyRepository companyRepository,
                            StockPriceRepository stockPriceRepository,
                            UserRepository userRepository) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistStockRepository = watchlistStockRepository;
        this.companyRepository = companyRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get summary of all watchlists for a user.
     */
    @Transactional(readOnly = true)
    public List<WatchlistSummaryResponse> getWatchlists(UUID userId) {
        List<Watchlist> watchlists = watchlistRepository.findByUserIdOrderBySortOrder(userId);
        return watchlists.stream()
                .map(w -> {
                    long stockCount = watchlistStockRepository.countByWatchlistId(w.getId());
                    return new WatchlistSummaryResponse(
                            w.getId(),
                            w.getName(),
                            w.getDescription(),
                            (int) stockCount,
                            w.isDefault(),
                            w.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Get detailed view of a watchlist with stock prices.
     */
    @Transactional(readOnly = true)
    public WatchlistDetailResponse getWatchlistDetail(UUID watchlistId, UUID userId) {
        Watchlist watchlist = getWatchlist(watchlistId, userId);
        List<WatchlistStock> stocks = watchlistStockRepository
                .findByWatchlistIdOrderBySortOrder(watchlistId);

        // Batch lookup companies and prices
        List<UUID> companyIds = stocks.stream()
                .map(WatchlistStock::getCompanyId)
                .collect(Collectors.toList());

        Map<UUID, Company> companyMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        List<StockPrice> latestPrices = stockPriceRepository.findLatestPrices(companyIds);
        Map<UUID, StockPrice> priceMap = latestPrices.stream()
                .collect(Collectors.toMap(StockPrice::getCompanyId, p -> p));

        List<WatchlistStockSummary> stockSummaries = new ArrayList<>();
        for (WatchlistStock ws : stocks) {
            Company company = companyMap.get(ws.getCompanyId());
            StockPrice price = priceMap.get(ws.getCompanyId());

            BigDecimal latestPrice = price != null ? price.getClose() : BigDecimal.ZERO;
            BigDecimal previousClose = getPreviousClose(ws.getCompanyId(), price);
            BigDecimal priceChange = latestPrice.subtract(previousClose);
            BigDecimal priceChangePercent = previousClose.compareTo(BigDecimal.ZERO) != 0
                    ? priceChange.divide(previousClose, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            stockSummaries.add(new WatchlistStockSummary(
                    company != null ? company.getTicker() : "N/A",
                    company != null ? company.getName() : "Unknown",
                    latestPrice,
                    priceChange,
                    priceChangePercent,
                    company != null ? company.getMarketCap() : null,
                    ws.getAddedAt(),
                    ws.getSortOrder()
            ));
        }

        return new WatchlistDetailResponse(
                watchlist.getId(),
                watchlist.getName(),
                watchlist.getDescription(),
                stockSummaries
        );
    }

    /**
     * Create a new watchlist. FREE tier limited to 1.
     */
    public WatchlistResponse createWatchlist(UUID userId, CreateWatchlistRequest request) {
        long count = watchlistRepository.countByUserId(userId);
        if (count >= FREE_WATCHLIST_LIMIT) {
            throw new WatchlistLimitExceededException(
                    "Free tier limited to " + FREE_WATCHLIST_LIMIT + " watchlist");
        }

        Watchlist watchlist = Watchlist.builder()
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .isDefault(count == 0)
                .sortOrder((int) count)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Watchlist saved = watchlistRepository.save(watchlist);

        return new WatchlistResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.isDefault(),
                saved.getSortOrder()
        );
    }

    /**
     * Update a watchlist name/description.
     */
    public WatchlistResponse updateWatchlist(UUID id, UUID userId, UpdateWatchlistRequest request) {
        Watchlist watchlist = getWatchlist(id, userId);

        if (request.name() != null) {
            watchlist.setName(request.name());
        }
        if (request.description() != null) {
            watchlist.setDescription(request.description());
        }
        watchlist.setUpdatedAt(Instant.now());

        Watchlist saved = watchlistRepository.save(watchlist);

        return new WatchlistResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.isDefault(),
                saved.getSortOrder()
        );
    }

    /**
     * Delete a watchlist (cascades to stocks).
     */
    public void deleteWatchlist(UUID id, UUID userId) {
        Watchlist watchlist = getWatchlist(id, userId);
        // Manually delete stocks first since we don't have cascade configured
        List<WatchlistStock> stocks = watchlistStockRepository.findByWatchlistIdOrderBySortOrder(id);
        watchlistStockRepository.deleteAll(stocks);
        watchlistRepository.delete(watchlist);
    }

    /**
     * Add a stock to a watchlist. Enforces 10 stock limit. Checks duplicate.
     */
    public void addStock(UUID watchlistId, UUID userId, String ticker) {
        Watchlist watchlist = getWatchlist(watchlistId, userId);
        Company company = companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));

        // Check stock limit
        long stockCount = watchlistStockRepository.countByWatchlistId(watchlistId);
        if (stockCount >= FREE_STOCK_LIMIT) {
            throw new WatchlistLimitExceededException(
                    "Free tier limited to " + FREE_STOCK_LIMIT + " stocks per watchlist");
        }

        // Check duplicate
        if (watchlistStockRepository.existsByWatchlistIdAndCompanyId(watchlistId, company.getId())) {
            throw new DuplicateStockException(
                    "Stock " + ticker + " is already in this watchlist");
        }

        // Get next sort order
        int maxSortOrder = watchlistStockRepository.getMaxSortOrder(watchlistId);

        WatchlistStock stock = WatchlistStock.builder()
                .watchlistId(watchlistId)
                .companyId(company.getId())
                .addedAt(Instant.now())
                .sortOrder(maxSortOrder + 1)
                .build();

        watchlistStockRepository.save(stock);
    }

    /**
     * Remove a stock from a watchlist.
     */
    public void removeStock(UUID watchlistId, UUID userId, String ticker) {
        Watchlist watchlist = getWatchlist(watchlistId, userId);
        Company company = companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));

        watchlistStockRepository.deleteByWatchlistIdAndCompanyId(watchlistId, company.getId());
    }

    /**
     * Reorder stocks in a watchlist.
     */
    public void reorderStocks(UUID watchlistId, UUID userId, List<String> orderedTickers) {
        Watchlist watchlist = getWatchlist(watchlistId, userId);

        // Fetch all stocks in the watchlist
        List<WatchlistStock> stocks = watchlistStockRepository
                .findByWatchlistIdOrderBySortOrder(watchlistId);

        // Build company lookup from ticker
        Map<String, Company> companyByTicker = companyRepository
                .findByTickerIn(orderedTickers.stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(c -> c.getTicker().toUpperCase(), c -> c));

        // Update sort order for each stock based on position in orderedTickers
        for (WatchlistStock stock : stocks) {
            Company company = companyByTicker.values().stream()
                    .filter(c -> c.getId().equals(stock.getCompanyId()))
                    .findFirst()
                    .orElse(null);
            if (company != null) {
                int newOrder = orderedTickers.indexOf(company.getTicker().toUpperCase());
                if (newOrder >= 0) {
                    stock.setSortOrder(newOrder);
                }
            }
        }

        watchlistStockRepository.saveAll(stocks);
    }

    // --- Private helpers ---

    private Watchlist getWatchlist(UUID watchlistId, UUID userId) {
        return watchlistRepository.findByIdAndUserId(watchlistId, userId)
                .orElseThrow(() -> new WatchlistNotFoundException(watchlistId));
    }

    private BigDecimal getPreviousClose(UUID companyId, StockPrice latestPrice) {
        if (latestPrice == null) {
            return BigDecimal.ZERO;
        }
        List<StockPrice> prices = stockPriceRepository.findByCompanyIdAndDateBetweenOrderByDateDesc(
                companyId,
                latestPrice.getDate().minusDays(5),
                latestPrice.getDate().minusDays(1),
                PageRequest.of(0, 1));
        return prices.isEmpty() ? latestPrice.getOpen() : prices.get(0).getClose();
    }
}
