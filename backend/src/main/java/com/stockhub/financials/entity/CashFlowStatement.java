package com.stockhub.financials.entity;

import com.stockhub.common.enums.PeriodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cash_flow_statements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "fiscal_date_ending", nullable = false)
    private LocalDate fiscalDateEnding;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(name = "operating_cash_flow", precision = 20, scale = 2)
    private BigDecimal operatingCashFlow;

    @Column(name = "capital_expenditure", precision = 20, scale = 2)
    private BigDecimal capitalExpenditure;

    @Column(name = "free_cash_flow", precision = 20, scale = 2)
    private BigDecimal freeCashFlow;

    @Column(name = "cash_flow_investing", precision = 20, scale = 2)
    private BigDecimal cashFlowInvesting;

    @Column(name = "cash_flow_financing", precision = 20, scale = 2)
    private BigDecimal cashFlowFinancing;

    @Column(name = "dividends_paid", precision = 20, scale = 2)
    private BigDecimal dividendsPaid;

    @Column(name = "stock_issuance", precision = 20, scale = 2)
    private BigDecimal stockIssuance;

    @Column(name = "debt_issuance", precision = 20, scale = 2)
    private BigDecimal debtIssuance;

    @Column(name = "net_change_in_cash", precision = 20, scale = 2)
    private BigDecimal netChangeInCash;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
