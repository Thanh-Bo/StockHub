package com.stockhub.common.exception;

public class CompanyNotFoundException extends ResourceNotFoundException {

    public CompanyNotFoundException(String ticker) {
        super("Company", "ticker", ticker);
    }
}
