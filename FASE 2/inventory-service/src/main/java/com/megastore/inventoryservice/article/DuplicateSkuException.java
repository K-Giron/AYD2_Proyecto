package com.megastore.inventoryservice.article;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("Ya existe un artículo con SKU " + sku);
    }
}
