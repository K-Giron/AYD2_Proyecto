package com.megastore.sgologic.logistics;

/** El pedido no se puede entregar en esa tienda (no ha llegado, no es de esa tienda o ya se entregó). */
public class PickupHandoffException extends RuntimeException {

    public PickupHandoffException(String message) {
        super(message);
    }
}
