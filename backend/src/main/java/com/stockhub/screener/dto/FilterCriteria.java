package com.stockhub.screener.dto;

import com.stockhub.common.enums.FilterOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record FilterCriteria(
    @NotBlank String field,
    @NotNull FilterOperator operator,
    String value,
    String minValue,
    String maxValue,
    @Size(max = 20) List<String> values
) {
    public BigDecimal getValueAsBigDecimal() {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }

    public BigDecimal getMinValueAsBigDecimal() {
        if (minValue == null || minValue.isBlank()) {
            return null;
        }
        return new BigDecimal(minValue);
    }

    public BigDecimal getMaxValueAsBigDecimal() {
        if (maxValue == null || maxValue.isBlank()) {
            return null;
        }
        return new BigDecimal(maxValue);
    }
}
