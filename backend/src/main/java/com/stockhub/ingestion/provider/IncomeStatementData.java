package com.stockhub.ingestion.provider;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data record carrying income statement fields from a provider.
 */
public record IncomeStatementData(
        LocalDate fiscalDateEnding,
        String periodType,
        Integer fiscalYear,
        Integer fiscalQuarter,
        BigDecimal totalRevenue,
        BigDecimal costOfRevenue,
        BigDecimal grossProfit,
        BigDecimal operatingExpense,
        BigDecimal operatingIncome,
        BigDecimal netIncome,
        BigDecimal eps,
        BigDecimal epsDiluted,
        BigDecimal interestExpense,
        BigDecimal incomeTaxExpense,
        BigDecimal ebitda,
        String reportUrl,
        LocalDate filingDate
) {
}
