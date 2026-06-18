package com.stockhub.comparison.dto;

import java.math.BigDecimal;

public record IndustryAveragesResponse(
    String sector,
    String industry,
    int companyCount,
    BigDecimal avgMarketCap,
    BigDecimal avgPE,
    BigDecimal avgRevenueGrowth,
    BigDecimal avgROE,
    BigDecimal avgDebtToEquity,
    BigDecimal avgNetMargin,
    BigDecimal pe25th,
    BigDecimal pe50th,
    BigDecimal pe75th
) {}
