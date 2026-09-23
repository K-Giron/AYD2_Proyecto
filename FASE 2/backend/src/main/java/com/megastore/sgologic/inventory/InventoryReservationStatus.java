package com.megastore.sgologic.inventory;

/**
 * Estado de una reserva de inventario (CDU-02). {@code ACTIVE} es el único
 * estado que mantiene comprometido el stock; {@code CONFIRMED} lo hará
 * definitivo cuando la Saga confirme la orden (ORD-03); {@code RELEASED} y
 * {@code EXPIRED} liberan la cantidad reservada (INV-03/ORD-04).
 */
public enum InventoryReservationStatus {
    ACTIVE,
    CONFIRMED,
    RELEASED,
    EXPIRED
}
