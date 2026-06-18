package com.stockhub.ingestion.provider;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * SEC EDGAR financial data provider.
 * <p>
 * This provider fetches data from the SEC EDGAR API
 * (base URL: {@code https://efts.sec.gov/LATEST/}).
 * </p>
 * <p>
 * Currently implemented as a stub because the SEC EDGAR API requires
 * real API access and credential configuration.
 * </p>
 */
@Component("secEdgarProvider")
public class SecEdgarProvider implements FinancialDataProvider {

    private static final String PROVIDER_NAME = "SEC_EDGAR";
    private static final String STUB_MESSAGE =
            "SEC EDGAR provider requires SEC API access. Configure credentials.";

    @Override
    public CompanyProfileData fetchCompanyProfile(String ticker) throws DataFetchException {
        throw new DataFetchException(ticker, PROVIDER_NAME, STUB_MESSAGE);
    }

    @Override
    public List<StockPriceData> fetchPriceHistory(String ticker, LocalDate from, LocalDate to)
            throws DataFetchException {
        throw new DataFetchException(ticker, PROVIDER_NAME,
                "SEC EDGAR does not provide price data. Use a market data provider (e.g., Yahoo Finance).");
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
        // SEC EDGAR supports all US-listed tickers
        return ticker != null && ticker.matches("^[A-Za-z]{1,5}$");
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
