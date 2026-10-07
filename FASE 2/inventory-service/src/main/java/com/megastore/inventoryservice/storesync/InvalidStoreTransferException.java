package com.megastore.inventoryservice.storesync;

public class InvalidStoreTransferException extends RuntimeException {

    public InvalidStoreTransferException(String message) {
        super(message);
    }
}
