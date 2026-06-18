package com.stockhub.watchlist.dto;

import java.util.List;
import java.util.UUID;

public record WatchlistDetailResponse(
    UUID id,
    String name,
    String description,
    List<WatchlistStockSummary> stocks
) {}
