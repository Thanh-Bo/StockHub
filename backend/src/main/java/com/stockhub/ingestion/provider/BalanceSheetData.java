package com.stockhub.ingestion.provider;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data record carrying balance sheet fields from a provider.
 */
public record BalanceSheetData(
        LocalDate fiscalDateEnding,
        String periodType,
        Integer fiscalYear,
        Integer fiscalQuarter,
        BigDecimal totalAssets,
        BigDecimal totalCurrentAssets,
        BigDecimal cashAndEquivalents,
        BigDecimal totalLiabilities,
        BigDecimal totalCurrentLiabilities,
        BigDecimal longTermDebt,
        BigDecimal totalDebt,
        BigDecimal totalShareholderEquity,
        BigDecimal retainedEarnings,
        BigDecimal treasuryStock,
        Long sharesOutstanding,
        String reportUrl,
        LocalDate filingDate
) {
}
