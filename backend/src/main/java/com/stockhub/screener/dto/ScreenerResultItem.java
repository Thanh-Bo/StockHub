package com.stockhub.screener.dto;

import java.math.BigDecimal;

public record ScreenerResultItem(
    String ticker,
    String name,
    String sector,
    String industry,
    BigDecimal marketCap,
    BigDecimal peRatio,
    BigDecimal revenueGrowthYoY,
    BigDecimal roe,
    BigDecimal dividendYield,
    BigDecimal debtToEquity,
    BigDecimal netMargin
) {}
