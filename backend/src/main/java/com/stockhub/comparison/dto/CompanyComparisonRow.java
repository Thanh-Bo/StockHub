package com.stockhub.comparison.dto;

import java.math.BigDecimal;
import java.util.Map;

public record CompanyComparisonRow(
    String ticker,
    String name,
    String sector,
    String industry,
    Map<String, BigDecimal> metrics
) {}
