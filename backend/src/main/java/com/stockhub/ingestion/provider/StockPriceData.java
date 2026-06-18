package com.stockhub.ingestion.provider;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data record carrying a single day's stock price data from a provider.
 */
public record StockPriceData(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        Long volume
) {
}
