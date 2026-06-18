package com.stockhub.financials.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceSheetResponse(
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
    LocalDate fiscalDateEnding,
    Integer fiscalYear,
    Integer fiscalQuarter,
    String periodType,
    LocalDate filingDate
) {}
