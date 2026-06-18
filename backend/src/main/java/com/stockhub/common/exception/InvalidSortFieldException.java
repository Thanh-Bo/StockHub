package com.stockhub.common.exception;

public class InvalidSortFieldException extends RuntimeException {

    public InvalidSortFieldException(String field) {
        super(String.format("Invalid sort field: %s", field));
    }
}
