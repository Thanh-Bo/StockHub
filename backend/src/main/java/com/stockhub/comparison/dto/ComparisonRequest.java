package com.stockhub.comparison.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ComparisonRequest(
    @NotEmpty @Size(min = 2, max = 5)
    List<@Pattern(regexp = "^[A-Z]{1,5}$") String> tickers,
    boolean includeIndustryAverages
) {
    public ComparisonRequest {
        if (tickers == null || tickers.isEmpty()) {
            throw new IllegalArgumentException("At least 2 tickers are required");
        }
    }

    public ComparisonRequest(List<String> tickers) {
        this(tickers, true);
    }
}
