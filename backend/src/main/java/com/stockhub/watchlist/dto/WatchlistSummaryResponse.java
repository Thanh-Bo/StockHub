package com.stockhub.watchlist.dto;

import java.time.Instant;
import java.util.UUID;

public record WatchlistSummaryResponse(
    UUID id,
    String name,
    String description,
    int stockCount,
    boolean isDefault,
    Instant createdAt
) {}
