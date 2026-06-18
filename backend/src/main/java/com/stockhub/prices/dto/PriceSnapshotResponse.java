package com.stockhub.prices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceSnapshotResponse(
    String ticker,
    BigDecimal currentPrice,
    BigDecimal priceChange,
    BigDecimal priceChangePercent,
    BigDecimal dayHigh,
    BigDecimal dayLow,
    BigDecimal previousClose,
    BigDecimal open,
    Long volume,
    LocalDate date
) {}
