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
@Table(name = "balance_sheets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheet {

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

    @Column(name = "total_assets", precision = 20, scale = 2)
    private BigDecimal totalAssets;

    @Column(name = "total_current_assets", precision = 20, scale = 2)
    private BigDecimal totalCurrentAssets;

    @Column(name = "cash_and_equivalents", precision = 20, scale = 2)
    private BigDecimal cashAndEquivalents;

    @Column(name = "total_liabilities", precision = 20, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(name = "total_current_liabilities", precision = 20, scale = 2)
    private BigDecimal totalCurrentLiabilities;

    @Column(name = "long_term_debt", precision = 20, scale = 2)
    private BigDecimal longTermDebt;

    @Column(name = "total_debt", precision = 20, scale = 2)
    private BigDecimal totalDebt;

    @Column(name = "total_shareholder_equity", precision = 20, scale = 2)
    private BigDecimal totalShareholderEquity;

    @Column(name = "retained_earnings", precision = 20, scale = 2)
    private BigDecimal retainedEarnings;

    @Column(name = "treasury_stock", precision = 20, scale = 2)
    private BigDecimal treasuryStock;

    @Column(name = "shares_outstanding")
    private Long sharesOutstanding;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
