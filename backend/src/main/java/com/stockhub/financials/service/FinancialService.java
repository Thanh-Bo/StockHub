package com.stockhub.financials.service;

import com.stockhub.common.dto.PagedResponse;
import com.stockhub.common.enums.PeriodType;
import com.stockhub.common.exception.CompanyNotFoundException;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.financials.dto.BalanceSheetResponse;
import com.stockhub.financials.dto.CashFlowStatementResponse;
import com.stockhub.financials.dto.IncomeStatementResponse;
import com.stockhub.financials.entity.BalanceSheet;
import com.stockhub.financials.entity.CashFlowStatement;
import com.stockhub.financials.entity.IncomeStatement;
import com.stockhub.financials.repository.BalanceSheetRepository;
import com.stockhub.financials.repository.CashFlowStatementRepository;
import com.stockhub.financials.repository.IncomeStatementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FinancialService {

    private final IncomeStatementRepository incomeStatementRepository;
    private final BalanceSheetRepository balanceSheetRepository;
    private final CashFlowStatementRepository cashFlowStatementRepository;
    private final CompanyRepository companyRepository;

    public FinancialService(IncomeStatementRepository incomeStatementRepository,
                            BalanceSheetRepository balanceSheetRepository,
                            CashFlowStatementRepository cashFlowStatementRepository,
                            CompanyRepository companyRepository) {
        this.incomeStatementRepository = incomeStatementRepository;
        this.balanceSheetRepository = balanceSheetRepository;
        this.cashFlowStatementRepository = cashFlowStatementRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Get paginated income statements for a ticker, filtered by period type and years back.
     */
    public PagedResponse<IncomeStatementResponse> getIncomeStatements(
            String ticker, PeriodType period, int years, int page, int size) {
        Company company = getCompany(ticker);
        int cutoffYear = Year.now().getValue() - years;

        // Fetch all matching statements (ordered by fiscal date desc), then apply year filter
        List<IncomeStatement> allStatements = incomeStatementRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        company.getId(), period, PageRequest.of(0, Integer.MAX_VALUE));

        List<IncomeStatement> filtered = allStatements.stream()
                .filter(is -> is.getFiscalYear() >= cutoffYear)
                .collect(Collectors.toList());

        return paginate(filtered, page, size, this::mapIncomeStatementResponse);
    }

    /**
     * Get paginated balance sheets for a ticker, filtered by period type and years back.
     */
    public PagedResponse<BalanceSheetResponse> getBalanceSheets(
            String ticker, PeriodType period, int years, int page, int size) {
        Company company = getCompany(ticker);
        int cutoffYear = Year.now().getValue() - years;

        List<BalanceSheet> allStatements = balanceSheetRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        company.getId(), period, PageRequest.of(0, Integer.MAX_VALUE));

        List<BalanceSheet> filtered = allStatements.stream()
                .filter(bs -> bs.getFiscalYear() >= cutoffYear)
                .collect(Collectors.toList());

        return paginate(filtered, page, size, this::mapBalanceSheetResponse);
    }

    /**
     * Get paginated cash flow statements for a ticker, filtered by period type and years back.
     */
    public PagedResponse<CashFlowStatementResponse> getCashFlowStatements(
            String ticker, PeriodType period, int years, int page, int size) {
        Company company = getCompany(ticker);
        int cutoffYear = Year.now().getValue() - years;

        List<CashFlowStatement> allStatements = cashFlowStatementRepository
                .findByCompanyIdAndPeriodTypeOrderByFiscalDateEndingDesc(
                        company.getId(), period, PageRequest.of(0, Integer.MAX_VALUE));

        List<CashFlowStatement> filtered = allStatements.stream()
                .filter(cf -> cf.getFiscalYear() >= cutoffYear)
                .collect(Collectors.toList());

        return paginate(filtered, page, size, this::mapCashFlowStatementResponse);
    }

    // --- Private helpers ---

    private Company getCompany(String ticker) {
        return companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));
    }

    private <E, R> PagedResponse<R> paginate(List<E> all, int page, int size,
                                             java.util.function.Function<E, R> mapper) {
        int totalElements = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<R> content = all.subList(fromIndex, toIndex).stream()
                .map(mapper)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }

    private IncomeStatementResponse mapIncomeStatementResponse(IncomeStatement is) {
        BigDecimal grossMargin = calculateMargin(is.getGrossProfit(), is.getTotalRevenue());
        BigDecimal operatingMargin = calculateMargin(is.getOperatingIncome(), is.getTotalRevenue());
        BigDecimal netMargin = calculateMargin(is.getNetIncome(), is.getTotalRevenue());

        return new IncomeStatementResponse(
                is.getTotalRevenue(),
                is.getCostOfRevenue(),
                is.getGrossProfit(),
                is.getOperatingExpense(),
                is.getOperatingIncome(),
                is.getNetIncome(),
                is.getEps(),
                is.getEpsDiluted(),
                is.getInterestExpense(),
                is.getIncomeTaxExpense(),
                is.getEbitda(),
                grossMargin,
                operatingMargin,
                netMargin,
                is.getFiscalDateEnding(),
                is.getFiscalYear(),
                is.getFiscalQuarter(),
                is.getPeriodType() != null ? is.getPeriodType().name() : null,
                is.getFilingDate()
        );
    }

    private BalanceSheetResponse mapBalanceSheetResponse(BalanceSheet bs) {
        return new BalanceSheetResponse(
                bs.getTotalAssets(),
                bs.getTotalCurrentAssets(),
                bs.getCashAndEquivalents(),
                bs.getTotalLiabilities(),
                bs.getTotalCurrentLiabilities(),
                bs.getLongTermDebt(),
                bs.getTotalDebt(),
                bs.getTotalShareholderEquity(),
                bs.getRetainedEarnings(),
                bs.getTreasuryStock(),
                bs.getSharesOutstanding(),
                bs.getFiscalDateEnding(),
                bs.getFiscalYear(),
                bs.getFiscalQuarter(),
                bs.getPeriodType() != null ? bs.getPeriodType().name() : null,
                bs.getFilingDate()
        );
    }

    private CashFlowStatementResponse mapCashFlowStatementResponse(CashFlowStatement cf) {
        return new CashFlowStatementResponse(
                cf.getOperatingCashFlow(),
                cf.getCapitalExpenditure(),
                cf.getFreeCashFlow(),
                cf.getCashFlowInvesting(),
                cf.getCashFlowFinancing(),
                cf.getDividendsPaid(),
                cf.getStockIssuance(),
                cf.getDebtIssuance(),
                cf.getNetChangeInCash(),
                cf.getFiscalDateEnding(),
                cf.getFiscalYear(),
                cf.getFiscalQuarter(),
                cf.getPeriodType() != null ? cf.getPeriodType().name() : null,
                cf.getFilingDate()
        );
    }

    private BigDecimal calculateMargin(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
