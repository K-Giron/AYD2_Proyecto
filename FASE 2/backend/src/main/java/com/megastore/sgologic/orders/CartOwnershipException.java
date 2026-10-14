package com.megastore.sgologic.orders;

import java.util.UUID;

class CartOwnershipException extends RuntimeException {
    CartOwnershipException(UUID cartId) {
        super("El carrito " + cartId + " no pertenece al cliente autenticado.");
    }
}
