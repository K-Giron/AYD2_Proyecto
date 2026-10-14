package com.megastore.storeservice.shared;

import org.springframework.http.HttpStatus;

/** Error de negocio de la tienda con su código y estado HTTP para la API local. */
public class StoreBusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public StoreBusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
