package com.megastore.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Almacén central: login por rol, artículos, existencias, movimientos de
 * entrada/salida con folio, cuentas por pagar/cobrar y seguimiento de
 * envíos. Sistema propio, con su propia base de datos.
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
