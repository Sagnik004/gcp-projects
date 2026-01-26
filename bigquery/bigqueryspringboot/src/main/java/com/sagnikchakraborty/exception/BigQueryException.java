package com.sagnikchakraborty.exception;

public class BigQueryException extends RuntimeException {

    public BigQueryException(String message) {
        super(message);
    }

    public BigQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
