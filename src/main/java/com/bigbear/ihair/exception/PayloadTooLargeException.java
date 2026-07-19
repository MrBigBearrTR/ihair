package com.bigbear.ihair.exception;

public class PayloadTooLargeException extends RuntimeException {
    public PayloadTooLargeException(String message) {
        super(message);
    }
}
