package com.megastore.sgologic.customers;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String username) {
        super("No existe un cliente con el usuario " + username + ".");
    }
}
