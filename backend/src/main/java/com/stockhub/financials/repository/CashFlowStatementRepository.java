package com.stockhub.financials.repository;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.financials.entity.CashFlowStatement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CashFlowStatement} entity.
 */
@Repository
public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatement, UUID> {

    /**
     * Find cash flow statements for a company filtered by period type, ordered by fiscal date descending.
     *
     * @param companyId  the company UUID
     * @param periodType the reporting period type (ANNUAL, QUARTERLY)
     * @param pageable   pagination information
     * @return paginated list of cash flow statements
     */
    List<CashFlowStatement> findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
            UUID companyId, PeriodType periodType, Pageable pageable);
}
