package com.bigbear.ihair.exception;

import lombok.Getter;

@Getter
public class SalonScopeException extends BadRequestException {

    private final String code;

    public SalonScopeException(String code, String message) {
        super(message);
        this.code = code;
    }
}
