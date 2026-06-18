package com.stockhub.metrics.entity;

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
@Table(name = "financial_ratios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRatio {

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

    @Column(name = "revenue_growth_yoy", precision = 10, scale = 4)
    private BigDecimal revenueGrowthYoY;

    @Column(name = "revenue_growth_3y", precision = 10, scale = 4)
    private BigDecimal revenueGrowth3y;

    @Column(name = "revenue_growth_5y", precision = 10, scale = 4)
    private BigDecimal revenueGrowth5y;

    @Column(name = "eps_growth_yoy", precision = 10, scale = 4)
    private BigDecimal epsGrowthYoY;

    @Column(name = "fcf_growth_yoy", precision = 10, scale = 4)
    private BigDecimal fcfGrowthYoY;

    @Column(precision = 10, scale = 4)
    private BigDecimal roe;

    @Column(precision = 10, scale = 4)
    private BigDecimal roa;

    @Column(name = "debt_to_equity", precision = 10, scale = 4)
    private BigDecimal debtToEquity;

    @Column(name = "gross_margin", precision = 10, scale = 4)
    private BigDecimal grossMargin;

    @Column(name = "operating_margin", precision = 10, scale = 4)
    private BigDecimal operatingMargin;

    @Column(name = "net_margin", precision = 10, scale = 4)
    private BigDecimal netMargin;

    @Column(name = "pe_ratio", precision = 10, scale = 4)
    private BigDecimal peRatio;

    @Column(name = "peg_ratio", precision = 10, scale = 4)
    private BigDecimal pegRatio;

    @Column(name = "dividend_yield", precision = 10, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "price_to_book", precision = 10, scale = 4)
    private BigDecimal priceToBook;

    @Column(name = "current_ratio", precision = 10, scale = 4)
    private BigDecimal currentRatio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
