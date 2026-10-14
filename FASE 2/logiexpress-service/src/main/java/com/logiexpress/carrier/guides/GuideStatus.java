package com.logiexpress.carrier.guides;

/**
 * CREADA (guía emitida, espera recolección) → EN_TRANSITO (el mensajero la
 * recogió en el origen) → ENTREGADA (domicilio) o EN_PUNTO_DE_RETIRO
 * (dejada en la tienda donde el cliente la recoge).
 */
public enum GuideStatus {
    CREADA, EN_TRANSITO, EN_PUNTO_DE_RETIRO, ENTREGADA
}
