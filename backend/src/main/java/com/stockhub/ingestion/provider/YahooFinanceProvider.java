package com.stockhub.ingestion.provider;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Yahoo Finance financial data provider.
 * <p>
 * This provider fetches data from Yahoo Finance APIs
 * (v8 chart API for prices, v11 for financials).
 * </p>
 * <p>
 * Currently implemented as a stub because the Yahoo Finance API requires
 * API key and endpoint configuration.
 * </p>
 */
@Component("yahooFinanceProvider")
public class YahooFinanceProvider implements FinancialDataProvider {

    private static final String PROVIDER_NAME = "YAHOO_FINANCE";
    private static final String STUB_MESSAGE =
            "Yahoo Finance provider requires Yahoo Finance API configuration. "
            + "Set stockhub.ingestion.yahoo-finance.api-key for API access.";

    @Override
    public CompanyProfileData fetchCompanyProfile(String ticker) throws DataFetchException {
        throw new DataFetchException(ticker, PROVIDER_NAME, STUB_MESSAGE);
    }

    @Override
    public List<StockPriceData> fetchPriceHistory(String ticker, LocalDate from, LocalDate to)
            throws DataFetchException {
        throw new DataFetchException(ticker, PROVIDER_NAME, STUB_MESSAGE);
    }

    @Override
    public List<IncomeStatementData> fetchIncomeStatements(String ticker, int yearsBack)
            throws DataFetchException {
        throw new DataFetchException(ticker, PROVIDER_NAME, STUB_MESSAGE);
    }

    @Override
    public List<BalanceSheetData> fetchBalanceSheets(String ticker, int yearsBack)
            throws DataFetchException {
        throw new DataFetchException(ticker, PROVIDER_NAME, STUB_MESSAGE);
    }

    @Override
    public List<CashFlowStatementData> fetchCashFlowStatements(String ticker, int yearsBack)
            throws DataFetchException {
        throw new DataFetchException(ticker, PROVIDER_NAME, STUB_MESSAGE);
    }

    @Override
    public boolean supports(String ticker) {
        // Yahoo Finance supports virtually all global tickers
        return ticker != null && ticker.matches("^[A-Za-z0-9.-]+$");
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
