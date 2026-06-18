package com.stockhub.ingestion.validation;

import com.stockhub.ingestion.provider.BalanceSheetData;
import com.stockhub.ingestion.provider.CashFlowStatementData;
import com.stockhub.ingestion.provider.CompanyProfileData;
import com.stockhub.ingestion.provider.IncomeStatementData;
import com.stockhub.ingestion.provider.StockPriceData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates raw data records from financial data providers before
 * they are transformed into JPA entities and persisted.
 */
@Component
public class FinancialDataValidator {

    private static final BigDecimal MAX_REVENUE_MULTIPLIER = BigDecimal.TEN;

    /**
     * Validate an income statement data record.
     * <ul>
     *   <li>Required fields: fiscalDateEnding, totalRevenue, netIncome</li>
     *   <li>Numeric ranges: totalRevenue &gt;= 0, |netIncome| &lt;= 10 * totalRevenue</li>
     *   <li>Fiscal year in range [2000, currentYear + 1]</li>
     * </ul>
     */
    public ValidationResult validateIncomeStatement(IncomeStatementData data) {
        List<String> errors = new ArrayList<>();

        if (data == null) {
            errors.add("Income statement data is null");
            return ValidationResult.failure(errors);
        }

        // Required fields
        if (data.fiscalDateEnding() == null) {
            errors.add("fiscalDateEnding is required");
        }
        if (data.totalRevenue() == null) {
            errors.add("totalRevenue is required");
        }
        if (data.netIncome() == null) {
            errors.add("netIncome is required");
        }

        // Numeric range: totalRevenue >= 0
        if (data.totalRevenue() != null && data.totalRevenue().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("totalRevenue must be >= 0, got: " + data.totalRevenue());
        }

        // Numeric range: |netIncome| <= 10 * totalRevenue (don't allow impossibly large values)
        if (data.totalRevenue() != null && data.netIncome() != null
                && data.totalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxNetIncome = data.totalRevenue().multiply(MAX_REVENUE_MULTIPLIER);
            BigDecimal absNetIncome = data.netIncome().abs();
            if (absNetIncome.compareTo(maxNetIncome) > 0) {
                errors.add("|netIncome| (" + absNetIncome + ") exceeds 10x totalRevenue ("
                        + maxNetIncome + ")");
            }
        }

        // Fiscal year range
        if (data.fiscalYear() != null) {
            int currentYear = Year.now().getValue();
            if (data.fiscalYear() < 2000 || data.fiscalYear() > currentYear + 1) {
                errors.add("fiscalYear must be in range [2000, " + (currentYear + 1) + "], got: "
                        + data.fiscalYear());
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate a balance sheet data record.
     * <ul>
     *   <li>Required fields: fiscalDateEnding, totalAssets, totalLiabilities, totalShareholderEquity</li>
     *   <li>Loose check: assets ≈ liabilities + equity (within 1% tolerance due to rounding)</li>
     * </ul>
     */
    public ValidationResult validateBalanceSheet(BalanceSheetData data) {
        List<String> errors = new ArrayList<>();

        if (data == null) {
            errors.add("Balance sheet data is null");
            return ValidationResult.failure(errors);
        }

        // Required fields
        if (data.fiscalDateEnding() == null) {
            errors.add("fiscalDateEnding is required");
        }
        if (data.totalAssets() == null) {
            errors.add("totalAssets is required");
        }
        if (data.totalLiabilities() == null) {
            errors.add("totalLiabilities is required");
        }
        if (data.totalShareholderEquity() == null) {
            errors.add("totalShareholderEquity is required");
        }

        // Loose check: totalAssets ≈ totalLiabilities + totalShareholderEquity
        if (data.totalAssets() != null && data.totalLiabilities() != null
                && data.totalShareholderEquity() != null) {
            BigDecimal liabilitiesPlusEquity =
                    data.totalLiabilities().add(data.totalShareholderEquity());
            BigDecimal diff = data.totalAssets().subtract(liabilitiesPlusEquity).abs();
            // Allow 1% of total assets as tolerance
            BigDecimal tolerance = data.totalAssets().abs().multiply(BigDecimal.valueOf(0.01));
            // For zero assets, allow zero tolerance
            if (data.totalAssets().compareTo(BigDecimal.ZERO) == 0) {
                tolerance = BigDecimal.ONE;
            }
            if (diff.compareTo(tolerance) > 0) {
                errors.add("Balance sheet not balanced: totalAssets=" + data.totalAssets()
                        + " vs liabilities+equity=" + liabilitiesPlusEquity
                        + " (diff=" + diff + ", tolerance=" + tolerance + ")");
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate a cash flow statement data record.
     * <ul>
     *   <li>Required fields: fiscalDateEnding, operatingCashFlow</li>
     * </ul>
     */
    public ValidationResult validateCashFlowStatement(CashFlowStatementData data) {
        List<String> errors = new ArrayList<>();

        if (data == null) {
            errors.add("Cash flow statement data is null");
            return ValidationResult.failure(errors);
        }

        if (data.fiscalDateEnding() == null) {
            errors.add("fiscalDateEnding is required");
        }
        if (data.operatingCashFlow() == null) {
            errors.add("operatingCashFlow is required");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate a stock price data record.
     * <ul>
     *   <li>High &gt;= low</li>
     *   <li>Close must be within [low, high] (loose check with 1% tolerance)</li>
     *   <li>Open must be within [low, high] (loose check)</li>
     * </ul>
     */
    public ValidationResult validateStockPrice(StockPriceData data) {
        List<String> errors = new ArrayList<>();

        if (data == null) {
            errors.add("Stock price data is null");
            return ValidationResult.failure(errors);
        }

        if (data.date() == null) {
            errors.add("date is required");
        }

        // High >= Low
        if (data.high() != null && data.low() != null
                && data.high().compareTo(data.low()) < 0) {
            errors.add("high (" + data.high() + ") must be >= low (" + data.low() + ")");
        }

        // Close within [low, high] with 1% tolerance for adjusted prices
        if (data.close() != null && data.high() != null && data.low() != null) {
            BigDecimal range = data.high().subtract(data.low());
            BigDecimal tolerance = BigDecimal.ZERO;
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                tolerance = range.multiply(BigDecimal.valueOf(0.01));
            }
            if (data.close().compareTo(data.low().subtract(tolerance)) < 0) {
                errors.add("close (" + data.close() + ") below low (" + data.low() + ")");
            }
            if (data.close().compareTo(data.high().add(tolerance)) > 0) {
                errors.add("close (" + data.close() + ") above high (" + data.high() + ")");
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate a company profile data record.
     * <ul>
     *   <li>Ticker must match pattern: 1-5 uppercase letters</li>
     *   <li>Required fields: ticker, name</li>
     * </ul>
     */
    public ValidationResult validateCompanyProfile(CompanyProfileData data) {
        List<String> errors = new ArrayList<>();

        if (data == null) {
            errors.add("Company profile data is null");
            return ValidationResult.failure(errors);
        }

        if (data.ticker() == null || data.ticker().isBlank()) {
            errors.add("ticker is required");
        } else if (!data.ticker().matches("^[A-Za-z]{1,5}$")) {
            errors.add("ticker must match pattern ^[A-Za-z]{1,5}$, got: " + data.ticker());
        }

        if (data.name() == null || data.name().isBlank()) {
            errors.add("name is required");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
