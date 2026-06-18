package com.stockhub.common.exception;

public class WatchlistLimitExceededException extends RuntimeException {

    public WatchlistLimitExceededException(String message) {
        super(message);
    }
}
