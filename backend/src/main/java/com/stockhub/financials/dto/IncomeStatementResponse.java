package com.stockhub.financials.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeStatementResponse(
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
    BigDecimal grossMargin,
    BigDecimal operatingMargin,
    BigDecimal netMargin,
    LocalDate fiscalDateEnding,
    Integer fiscalYear,
    Integer fiscalQuarter,
    String periodType,
    LocalDate filingDate
) {}
