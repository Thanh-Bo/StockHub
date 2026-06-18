package com.stockhub.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.UUID;

@Entity
@Table(name = "industry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Industry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 100)
    private String sector;

    @Column(name = "industry_group", length = 100)
    private String industryGroup;

    @Column(length = 100)
    private String industry;

    @Column(name = "company_count", nullable = false)
    @Builder.Default
    private Integer companyCount = 0;

    @Column(name = "avg_market_cap", precision = 20, scale = 2)
    private BigDecimal avgMarketCap;

    @Column(name = "avg_pe_ratio", precision = 10, scale = 2)
    private BigDecimal avgPeRatio;

    @Column(name = "avg_revenue_growth", precision = 10, scale = 4)
    private BigDecimal avgRevenueGrowth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
