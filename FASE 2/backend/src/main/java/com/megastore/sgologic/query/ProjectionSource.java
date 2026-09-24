package com.megastore.sgologic.query;

/**
 * Áreas que alimentan la vista consolidada de CDU-09: compra, pago,
 * despacho, entrega e incidencia/devolución.
 */
public enum ProjectionSource {
    ORDER,
    PAYMENT,
    DISPATCH,
    DELIVERY,
    INCIDENT
}
