package com.megastore.sgologic.logistics;

/**
 * LogiExpress respondió indicando que el servicio no está disponible.
 */
public class LogiExpressUnavailableException extends Exception {

    public LogiExpressUnavailableException(String message) {
        super(message);
    }
}
