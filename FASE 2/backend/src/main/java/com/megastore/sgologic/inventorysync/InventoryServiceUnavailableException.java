package com.megastore.sgologic.inventorysync;

/**
 * Falla transitoria (red, timeout, login, 5xx) hablando con
 * inventory-service — se reintenta con backoff antes de darla por
 * perdida (ver {@code InventorySyncService}).
 */
public class InventoryServiceUnavailableException extends RuntimeException {

    public InventoryServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
