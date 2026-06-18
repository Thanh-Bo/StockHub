package com.stockhub.financials.repository;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.financials.entity.BalanceSheet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link BalanceSheet} entity.
 */
@Repository
public interface BalanceSheetRepository extends JpaRepository<BalanceSheet, UUID> {

    /**
     * Find balance sheets for a company filtered by period type, ordered by fiscal date descending.
     *
     * @param companyId  the company UUID
     * @param periodType the reporting period type (ANNUAL, QUARTERLY)
     * @param pageable   pagination information
     * @return paginated list of balance sheets
     */
    List<BalanceSheet> findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
            UUID companyId, PeriodType periodType, Pageable pageable);
}
