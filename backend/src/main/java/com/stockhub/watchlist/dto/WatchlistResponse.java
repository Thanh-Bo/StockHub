package com.stockhub.watchlist.dto;

import java.util.UUID;

public record WatchlistResponse(
    UUID id,
    String name,
    String description,
    boolean isDefault,
    int sortOrder
) {}
