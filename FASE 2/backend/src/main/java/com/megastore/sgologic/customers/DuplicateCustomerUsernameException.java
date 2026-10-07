package com.megastore.sgologic.customers;

public class DuplicateCustomerUsernameException extends RuntimeException {

    public DuplicateCustomerUsernameException(String username) {
        super("Ya existe un cliente con el usuario " + username + ".");
    }
}
