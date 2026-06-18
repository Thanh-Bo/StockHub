package com.stockhub.company.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardResponse(
    String ticker,
    String name,
    String description,
    String sector,
    String industry,
    String headquarters,
    BigDecimal marketCap,
    Long employees,
    BigDecimal currentPrice,
    BigDecimal priceChange,
    BigDecimal priceChangePercent,
    BigDecimal dayHigh,
    BigDecimal dayLow,
    BigDecimal previousClose,
    Long volume,
    List<PricePoint> priceHistory,
    BigDecimal revenueGrowthYoY,
    BigDecimal epsGrowthYoY,
    BigDecimal roe,
    BigDecimal roa,
    BigDecimal peRatio,
    BigDecimal grossMargin,
    BigDecimal netMargin,
    BigDecimal debtToEquity,
    BigDecimal dividendYield,
    IndustryContext industryContext,
    Instant lastUpdated,
    String dataSource
) {}
