package com.stockhub.screener.dto;

import java.math.BigDecimal;
import java.util.List;

public record FilterMetadata(
    String field,
    String label,
    String type,
    BigDecimal minValue,
    BigDecimal maxValue,
    List<String> options
) {}
