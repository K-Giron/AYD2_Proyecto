package com.megastore.sgologic.logistics;

/**
 * LogiExpress no respondió dentro del timeout definido (CDU-08, E1). Un
 * timeout nunca se interpreta como éxito.
 */
public class LogiExpressTimeoutException extends Exception {

    public LogiExpressTimeoutException(String message) {
        super(message);
    }
}
