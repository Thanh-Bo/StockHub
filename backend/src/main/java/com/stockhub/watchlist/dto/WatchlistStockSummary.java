package com.stockhub.watchlist.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WatchlistStockSummary(
    String ticker,
    String name,
    BigDecimal latestPrice,
    BigDecimal priceChange,
    BigDecimal priceChangePercent,
    BigDecimal marketCap,
    Instant addedAt,
    int sortOrder
) {}
