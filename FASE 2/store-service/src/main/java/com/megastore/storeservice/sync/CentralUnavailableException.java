package com.megastore.storeservice.sync;

/** Sin conexión con el central (red, caída o error 5xx): se reintenta después. */
public class CentralUnavailableException extends RuntimeException {

    public CentralUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
