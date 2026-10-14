package com.megastore.sgologic.inventorysync;

/**
 * El nombre del producto vendido en la web no tiene un artículo
 * correspondiente en el catálogo de inventory-service — problema de
 * datos entre los dos catálogos independientes, no de red. No se
 * reintenta: va directo a la bandeja de fallidos.
 */
public class InventoryArticleNotFoundException extends RuntimeException {

    public InventoryArticleNotFoundException(String productName) {
        super("No existe un artículo en el almacén central con el nombre \"" + productName + "\".");
    }
}
