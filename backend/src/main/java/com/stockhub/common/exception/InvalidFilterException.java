package com.stockhub.common.exception;

public class InvalidFilterException extends RuntimeException {

    public InvalidFilterException(String field) {
        super(String.format("Invalid filter field: %s", field));
    }
}
