package com.logiexpress.carrier.shared;

import org.springframework.http.HttpStatus;

/** Error de negocio con su código HTTP y un código estable para el comercio. */
public class CarrierException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public CarrierException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
