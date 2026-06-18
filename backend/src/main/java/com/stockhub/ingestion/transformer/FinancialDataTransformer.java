package com.stockhub.ingestion.transformer;

import com.stockhub.common.enums.PeriodType;
import com.stockhub.company.entity.Company;
import com.stockhub.financials.entity.BalanceSheet;
import com.stockhub.financials.entity.CashFlowStatement;
import com.stockhub.financials.entity.IncomeStatement;
import com.stockhub.ingestion.provider.BalanceSheetData;
import com.stockhub.ingestion.provider.CashFlowStatementData;
import com.stockhub.ingestion.provider.CompanyProfileData;
import com.stockhub.ingestion.provider.IncomeStatementData;
import com.stockhub.ingestion.provider.StockPriceData;
import com.stockhub.prices.entity.StockPrice;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Transforms provider DTOs/records into JPA entities.
 * All monetary values are normalized to millions where appropriate.
 */
@Component
public class FinancialDataTransformer {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    /**
     * Transform provider income statement data into a JPA entity.
     * All monetary values are kept in their raw units (no scaling).
     */
    public IncomeStatement toIncomeStatement(IncomeStatementData data, UUID companyId) {
        if (data == null) {
            return null;
        }
        return IncomeStatement.builder()
                .companyId(companyId)
                .fiscalDateEnding(data.fiscalDateEnding())
                .periodType(parsePeriodType(data.periodType()))
                .fiscalYear(data.fiscalYear())
                .fiscalQuarter(data.fiscalQuarter())
                .totalRevenue(data.totalRevenue())
                .costOfRevenue(data.costOfRevenue())
                .grossProfit(data.grossProfit())
                .operatingExpense(data.operatingExpense())
                .operatingIncome(data.operatingIncome())
                .netIncome(data.netIncome())
                .eps(data.eps())
                .epsDiluted(data.epsDiluted())
                .interestExpense(data.interestExpense())
                .incomeTaxExpense(data.incomeTaxExpense())
                .ebitda(data.ebitda())
                .reportUrl(data.reportUrl())
                .filingDate(data.filingDate())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Transform provider balance sheet data into a JPA entity.
     */
    public BalanceSheet toBalanceSheet(BalanceSheetData data, UUID companyId) {
        if (data == null) {
            return null;
        }
        return BalanceSheet.builder()
                .companyId(companyId)
                .fiscalDateEnding(data.fiscalDateEnding())
                .periodType(parsePeriodType(data.periodType()))
                .fiscalYear(data.fiscalYear())
                .fiscalQuarter(data.fiscalQuarter())
                .totalAssets(data.totalAssets())
                .totalCurrentAssets(data.totalCurrentAssets())
                .cashAndEquivalents(data.cashAndEquivalents())
                .totalLiabilities(data.totalLiabilities())
                .totalCurrentLiabilities(data.totalCurrentLiabilities())
                .longTermDebt(data.longTermDebt())
                .totalDebt(data.totalDebt())
                .totalShareholderEquity(data.totalShareholderEquity())
                .retainedEarnings(data.retainedEarnings())
                .treasuryStock(data.treasuryStock())
                .sharesOutstanding(data.sharesOutstanding())
                .reportUrl(data.reportUrl())
                .filingDate(data.filingDate())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Transform provider cash flow statement data into a JPA entity.
     */
    public CashFlowStatement toCashFlowStatement(CashFlowStatementData data, UUID companyId) {
        if (data == null) {
            return null;
        }
        return CashFlowStatement.builder()
                .companyId(companyId)
                .fiscalDateEnding(data.fiscalDateEnding())
                .periodType(parsePeriodType(data.periodType()))
                .fiscalYear(data.fiscalYear())
                .fiscalQuarter(data.fiscalQuarter())
                .operatingCashFlow(data.operatingCashFlow())
                .capitalExpenditure(data.capitalExpenditure())
                .freeCashFlow(data.freeCashFlow())
                .cashFlowInvesting(data.cashFlowInvesting())
                .cashFlowFinancing(data.cashFlowFinancing())
                .dividendsPaid(data.dividendsPaid())
                .stockIssuance(data.stockIssuance())
                .debtIssuance(data.debtIssuance())
                .netChangeInCash(data.netChangeInCash())
                .reportUrl(data.reportUrl())
                .filingDate(data.filingDate())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Transform provider stock price data into a JPA entity.
     */
    public StockPrice toStockPrice(StockPriceData data, UUID companyId) {
        if (data == null) {
            return null;
        }
        return StockPrice.builder()
                .companyId(companyId)
                .date(data.date())
                .open(data.open())
                .high(data.high())
                .low(data.low())
                .close(data.close())
                .adjustedClose(data.adjustedClose())
                .volume(data.volume())
                .build();
    }

    /**
     * Transform provider company profile data into a JPA entity.
     * The ticker is uppercased and string fields are mapped directly.
     */
    public Company toCompany(CompanyProfileData data) {
        if (data == null) {
            return null;
        }
        Integer employees = null;
        if (data.employees() != null && !data.employees().isBlank()) {
            try {
                employees = Integer.parseInt(data.employees());
            } catch (NumberFormatException e) {
                // ignore unparseable employee count
            }
        }

        Integer foundedYear = null;
        if (data.foundedYear() != null && !data.foundedYear().isBlank()) {
            try {
                foundedYear = Integer.parseInt(data.foundedYear());
            } catch (NumberFormatException e) {
                // ignore unparseable year
            }
        }

        return Company.builder()
                .ticker(data.ticker() != null ? data.ticker().toUpperCase() : null)
                .cik(data.cik())
                .name(data.name())
                .description(data.description())
                .sector(data.sector())
                .industry(data.industry())
                .employees(employees)
                .foundedYear(foundedYear)
                .headquarters(data.headquarters())
                .website(data.website())
                .marketCap(data.marketCap())
                .isActive(true)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Normalize a monetary value to millions.
     */
    public static BigDecimal normalizeToMillions(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.divide(ONE_MILLION, 2, RoundingMode.HALF_UP);
    }

    /**
     * Parse a period type string into the {@link PeriodType} enum.
     * Accepts "ANNUAL", "QUARTERLY", "FY", "Q", "10-K", "10-Q" etc.
     */
    private static PeriodType parsePeriodType(String periodType) {
        if (periodType == null) {
            return PeriodType.ANNUAL;
        }
        String upper = periodType.toUpperCase().trim();
        if (upper.contains("Q") && !upper.equals("QUARTERLY")) {
            return PeriodType.QUARTERLY;
        }
        if (upper.equals("QUARTERLY") || upper.equals("10-Q") || upper.equals("QTR")) {
            return PeriodType.QUARTERLY;
        }
        return PeriodType.ANNUAL;
    }
}
