package com.stockhub.common.exception;

import java.util.UUID;

public class WatchlistNotFoundException extends ResourceNotFoundException {

    public WatchlistNotFoundException(UUID id) {
        super("Watchlist", "id", id);
    }
}
