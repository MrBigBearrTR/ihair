package com.bigbear.ihair.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class OutsideWorkingHoursException extends RuntimeException {
    private final String reason;
    private final Map<String, Object> details;

    public OutsideWorkingHoursException(String message, String reason, Map<String, Object> details) {
        super(message);
        this.reason = reason;
        this.details = details;
    }
}
