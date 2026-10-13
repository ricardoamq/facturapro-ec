package com.facturapro.common.error;

public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
