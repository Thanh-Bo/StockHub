package com.stockhub.ingestion.provider;

/**
 * Exception thrown when a {@link FinancialDataProvider} fails to retrieve data.
 */
public class DataFetchException extends RuntimeException {

    private final String ticker;
    private final String provider;

    public DataFetchException(String ticker, String provider, String message, Throwable cause) {
        super(formatMessage(ticker, provider, message), cause);
        this.ticker = ticker;
        this.provider = provider;
    }

    public DataFetchException(String ticker, String provider, String message) {
        super(formatMessage(ticker, provider, message));
        this.ticker = ticker;
        this.provider = provider;
    }

    public String getTicker() {
        return ticker;
    }

    public String getProvider() {
        return provider;
    }

    private static String formatMessage(String ticker, String provider, String message) {
        return "Failed to fetch data for " + ticker + " from " + provider + ": " + message;
    }
}
