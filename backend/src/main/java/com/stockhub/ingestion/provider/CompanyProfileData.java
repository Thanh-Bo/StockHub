package com.stockhub.ingestion.provider;

import java.math.BigDecimal;

/**
 * Data record carrying company profile fields returned by a {@link FinancialDataProvider}.
 */
public record CompanyProfileData(
        String ticker,
        String cik,
        String name,
        String description,
        String sector,
        String industry,
        String employees,
        String foundedYear,
        String headquarters,
        String website,
        BigDecimal marketCap
) {
}
