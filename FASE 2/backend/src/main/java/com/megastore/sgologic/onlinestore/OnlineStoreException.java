package com.megastore.sgologic.onlinestore;

import org.springframework.http.HttpStatus;

/** Error de negocio de la tienda online, con su código y estado HTTP. */
public class OnlineStoreException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public OnlineStoreException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
