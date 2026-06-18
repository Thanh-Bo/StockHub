package com.stockhub.financials.repository;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.financials.entity.IncomeStatement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link IncomeStatement} entity.
 */
@Repository
public interface IncomeStatementRepository extends JpaRepository<IncomeStatement, UUID> {

    /**
     * Find income statements for a company filtered by period type, ordered by fiscal date descending.
     *
     * @param companyId  the company UUID
     * @param periodType the reporting period type (ANNUAL, QUARTERLY)
     * @param pageable   pagination information
     * @return paginated list of income statements
     */
    List<IncomeStatement> findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
            UUID companyId, PeriodType periodType, Pageable pageable);

    /**
     * Find a specific income statement by company, period type, and fiscal year.
     *
     * @param companyId  the company UUID
     * @param periodType the reporting period type
     * @param fiscalYear the fiscal year
     * @return an Optional containing the income statement if found
     */
    Optional<IncomeStatement> findByCompanyIdAndPeriodTypeAndFiscalYear(
            UUID companyId, PeriodType periodType, Integer fiscalYear);

    /**
     * Find all income statements for a company ordered by fiscal date descending.
     *
     * @param companyId the company UUID
     * @param pageable  pagination information
     * @return paginated list of income statements
     */
    List<IncomeStatement> findByCompanyIdOrderByFiscalDateEndingDesc(
            UUID companyId, Pageable pageable);
}
