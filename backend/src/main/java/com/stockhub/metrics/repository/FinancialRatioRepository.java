package com.stockhub.metrics.repository;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.metrics.entity.FinancialRatio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link FinancialRatio} entity.
 */
@Repository
public interface FinancialRatioRepository extends JpaRepository<FinancialRatio, UUID> {

    /**
     * Find financial ratios for a company filtered by period type, ordered by fiscal date descending.
     *
     * @param companyId  the company UUID
     * @param periodType the reporting period type (ANNUAL, QUARTERLY)
     * @param pageable   pagination information
     * @return paginated list of financial ratios
     */
    List<FinancialRatio> findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
            UUID companyId, PeriodType periodType, Pageable pageable);

    /**
     * Find a specific financial ratio by company, period type, and fiscal date ending.
     *
     * @param companyId        the company UUID
     * @param periodType       the reporting period type
     * @param fiscalDateEnding the fiscal date ending
     * @return an Optional containing the financial ratio if found
     */
    Optional<FinancialRatio> findByCompanyIdAndPeriodTypeAndFiscalDateEnding(
            UUID companyId, PeriodType periodType, LocalDate fiscalDateEnding);

    /**
     * Find the latest annual financial ratios for multiple companies using DISTINCT ON.
     * Returns only the most recent ANNUAL ratio for each specified company.
     *
     * @param companyIds list of company UUIDs
     * @return list of the latest annual FinancialRatio for each company
     */
    @Query(value = "SELECT DISTINCT ON (fr.company_id) fr.* FROM financial_ratios fr " +
                   "WHERE fr.company_id IN (:companyIds) AND fr.period_type = 'ANNUAL' " +
                   "ORDER BY fr.company_id, fr.fiscal_date_ending DESC",
           nativeQuery = true)
    List<FinancialRatio> findLatestAnnualRatios(@Param("companyIds") List<UUID> companyIds);
}
