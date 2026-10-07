package com.megastore.inventoryservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Una cuenta TIENDA solo sincroniza su propio punto de venta: la cuenta de
 * POS-01 no puede subir facturas ni sacar mercadería a nombre de POS-02.
 */
@Component
public class StoreAccountGuard {

    private final SecurityProperties properties;

    public StoreAccountGuard(SecurityProperties properties) {
        this.properties = properties;
    }

    public void assertCanActFor(String username, String pointOfSaleCode) {
        boolean allowed = properties.getUsers().stream()
                .anyMatch(user -> user.getUsername().equals(username)
                        && pointOfSaleCode.equals(user.getPointOfSaleCode()));
        if (!allowed) {
            throw new AccessDeniedException("La cuenta " + username + " no sincroniza el punto de venta " + pointOfSaleCode + ".");
        }
    }
}
