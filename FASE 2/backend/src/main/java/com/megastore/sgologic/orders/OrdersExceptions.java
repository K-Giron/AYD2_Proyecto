package com.megastore.sgologic.orders;

import java.util.UUID;

class CartNotFoundException extends RuntimeException {
    CartNotFoundException(UUID id) { super("No existe el carrito " + id + "."); }
}

class ActiveCartNotFoundException extends RuntimeException {
    ActiveCartNotFoundException(UUID customerId) {
        super("El cliente " + customerId + " no tiene un carrito activo.");
    }
}

class CartItemNotFoundException extends RuntimeException {
    CartItemNotFoundException(UUID cartId, UUID productId) {
        super("El producto " + productId + " no existe en el carrito " + cartId + ".");
    }
}

class CartNotActiveException extends RuntimeException {
    CartNotActiveException(UUID cartId, CartStatus status) {
        super("El carrito " + cartId + " no puede modificarse porque está en estado " + status + ".");
    }
}

class EmptyCartException extends RuntimeException {
    EmptyCartException(UUID cartId) { super("El carrito " + cartId + " no contiene productos."); }
}

class OrderNotFoundException extends RuntimeException {
    OrderNotFoundException(UUID id) { super("No existe la orden " + id + "."); }
}

class InvalidOrderTransitionException extends RuntimeException {
    InvalidOrderTransitionException(UUID orderId, OrderStatus current, OrderStatus next) {
        super("La orden " + orderId + " no admite la transición de " + current + " a " + next + ".");
    }
}
