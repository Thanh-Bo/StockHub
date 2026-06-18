package com.stockhub.metrics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MetricsResponse(
    String ticker,
    String name,
    LocalDate fiscalDateEnding,
    BigDecimal revenueGrowthYoY,
    BigDecimal revenueGrowth3y,
    BigDecimal revenueGrowth5y,
    BigDecimal epsGrowthYoY,
    BigDecimal fcfGrowthYoY,
    BigDecimal roe,
    BigDecimal roa,
    BigDecimal debtToEquity,
    BigDecimal grossMargin,
    BigDecimal operatingMargin,
    BigDecimal netMargin,
    BigDecimal peRatio,
    BigDecimal pegRatio,
    BigDecimal dividendYield,
    BigDecimal priceToBook,
    BigDecimal currentRatio
) {}
