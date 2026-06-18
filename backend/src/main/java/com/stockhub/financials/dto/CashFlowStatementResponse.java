package com.stockhub.financials.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowStatementResponse(
    BigDecimal operatingCashFlow,
    BigDecimal capitalExpenditure,
    BigDecimal freeCashFlow,
    BigDecimal cashFlowInvesting,
    BigDecimal cashFlowFinancing,
    BigDecimal dividendsPaid,
    BigDecimal stockIssuance,
    BigDecimal debtIssuance,
    BigDecimal netChangeInCash,
    LocalDate fiscalDateEnding,
    Integer fiscalYear,
    Integer fiscalQuarter,
    String periodType,
    LocalDate filingDate
) {}
