package com.facturapro.common.error;

public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
