package com.CalFX.exception;

/** Thrown when an expression typed by the user cannot be understood. */
public class ExpressionException extends RuntimeException {

    public ExpressionException(String message) {
        super(message);
    }
}
