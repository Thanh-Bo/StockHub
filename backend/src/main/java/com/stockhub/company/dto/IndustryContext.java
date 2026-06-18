package com.stockhub.company.dto;

import java.math.BigDecimal;

public record IndustryContext(
    String sector,
    String industry,
    BigDecimal avgPE,
    BigDecimal avgROE,
    BigDecimal avgRevenueGrowth,
    BigDecimal avgNetMargin,
    BigDecimal pePercentile,
    BigDecimal roePercentile
) {}
