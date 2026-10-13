package com.facturapro.common.error;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
