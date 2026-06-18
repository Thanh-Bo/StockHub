package com.stockhub.ingestion.provider;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data record carrying cash flow statement fields from a provider.
 */
public record CashFlowStatementData(
        LocalDate fiscalDateEnding,
        String periodType,
        Integer fiscalYear,
        Integer fiscalQuarter,
        BigDecimal operatingCashFlow,
        BigDecimal capitalExpenditure,
        BigDecimal freeCashFlow,
        BigDecimal cashFlowInvesting,
        BigDecimal cashFlowFinancing,
        BigDecimal dividendsPaid,
        BigDecimal stockIssuance,
        BigDecimal debtIssuance,
        BigDecimal netChangeInCash,
        String reportUrl,
        LocalDate filingDate
) {
}
