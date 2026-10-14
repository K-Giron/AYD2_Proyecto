package com.megastore.storeservice.sync;

/** El central respondió y no aceptó la solicitud (4xx): reintentar igual no sirve. */
public class CentralRejectedException extends RuntimeException {

    private final int status;

    public CentralRejectedException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
