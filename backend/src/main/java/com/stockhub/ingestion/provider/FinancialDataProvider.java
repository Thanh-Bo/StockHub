package com.stockhub.ingestion.provider;

import java.time.LocalDate;
import java.util.List;

/**
 * Core abstraction for financial data providers.
 * Swappable implementations behind this interface allow the application
 * to ingest data from multiple sources (SEC EDGAR, Yahoo Finance, etc.)
 * without changing business logic.
 */
public interface FinancialDataProvider {

    /**
     * Fetch company profile information for a given ticker.
     *
     * @param ticker the stock ticker symbol
     * @return populated {@link CompanyProfileData}
     * @throws DataFetchException if the data cannot be retrieved
     */
    CompanyProfileData fetchCompanyProfile(String ticker) throws DataFetchException;

    /**
     * Fetch historical daily price data within the given date range.
     *
     * @param ticker the stock ticker symbol
     * @param from   start date (inclusive)
     * @param to     end date (inclusive)
     * @return list of daily price records, ordered by date ascending
     * @throws DataFetchException if the data cannot be retrieved
     */
    List<StockPriceData> fetchPriceHistory(String ticker, LocalDate from, LocalDate to)
            throws DataFetchException;

    /**
     * Fetch income statements going back {@code yearsBack} years.
     *
     * @param ticker    the stock ticker symbol
     * @param yearsBack number of years of history to retrieve
     * @return list of income statement records
     * @throws DataFetchException if the data cannot be retrieved
     */
    List<IncomeStatementData> fetchIncomeStatements(String ticker, int yearsBack)
            throws DataFetchException;

    /**
     * Fetch balance sheets going back {@code yearsBack} years.
     *
     * @param ticker    the stock ticker symbol
     * @param yearsBack number of years of history to retrieve
     * @return list of balance sheet records
     * @throws DataFetchException if the data cannot be retrieved
     */
    List<BalanceSheetData> fetchBalanceSheets(String ticker, int yearsBack)
            throws DataFetchException;

    /**
     * Fetch cash flow statements going back {@code yearsBack} years.
     *
     * @param ticker    the stock ticker symbol
     * @param yearsBack number of years of history to retrieve
     * @return list of cash flow statement records
     * @throws DataFetchException if the data cannot be retrieved
     */
    List<CashFlowStatementData> fetchCashFlowStatements(String ticker, int yearsBack)
            throws DataFetchException;

    /**
     * Check whether this provider supports the given ticker.
     *
     * @param ticker the stock ticker symbol
     * @return {@code true} if this provider can serve data for this ticker
     */
    boolean supports(String ticker);

    /**
     * Return a human-readable name for this provider (e.g., "SEC_EDGAR").
     *
     * @return provider name
     */
    String getProviderName();
}
