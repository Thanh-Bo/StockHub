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
@Table(name = "income_statements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatement {

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

    @Column(name = "total_revenue", precision = 20, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "cost_of_revenue", precision = 20, scale = 2)
    private BigDecimal costOfRevenue;

    @Column(name = "gross_profit", precision = 20, scale = 2)
    private BigDecimal grossProfit;

    @Column(name = "operating_expense", precision = 20, scale = 2)
    private BigDecimal operatingExpense;

    @Column(name = "operating_income", precision = 20, scale = 2)
    private BigDecimal operatingIncome;

    @Column(name = "net_income", precision = 20, scale = 2)
    private BigDecimal netIncome;

    @Column(precision = 10, scale = 4)
    private BigDecimal eps;

    @Column(name = "eps_diluted", precision = 10, scale = 4)
    private BigDecimal epsDiluted;

    @Column(name = "interest_expense", precision = 20, scale = 2)
    private BigDecimal interestExpense;

    @Column(name = "income_tax_expense", precision = 20, scale = 2)
    private BigDecimal incomeTaxExpense;

    @Column(precision = 20, scale = 2)
    private BigDecimal ebitda;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
