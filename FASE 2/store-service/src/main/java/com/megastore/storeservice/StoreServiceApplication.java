package com.megastore.storeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Sistema propio de una tienda física: su almacén (lo que el central le
 * envió por folio), venta y cobro con NIT o CF, traslados por folio y la
 * sincronización con el almacén central. Vende aunque el central esté
 * caído; las facturas quedan pendientes y suben solas al volver.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreServiceApplication.class, args);
    }
}
