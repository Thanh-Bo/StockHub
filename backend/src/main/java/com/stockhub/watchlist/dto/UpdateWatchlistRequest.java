package com.stockhub.watchlist.dto;

import jakarta.validation.constraints.Size;

public record UpdateWatchlistRequest(
    @Size(max = 100) String name,
    @Size(max = 500) String description
) {}
