package com.stockhub.export.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for exporting financial data to Excel or PDF.
 */
public record ExportRequest(
        ExportType type,

        @Size(min = 1, max = 20, message = "Must provide 1-20 tickers")
        List<String> tickers,

        String statementType,

        @Pattern(regexp = "ANNUAL|QUARTERLY", message = "Period must be ANNUAL or QUARTERLY")
        String period,

        @Min(value = 1, message = "Minimum 1 year")
        @Max(value = 10, message = "Maximum 10 years")
        int years
) {
}
