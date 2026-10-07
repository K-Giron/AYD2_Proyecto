package com.payklik.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * PayKlik: la pasarela de pagos contratada por MegaStore. Autoriza cobros
 * con tarjeta tokenizada o transferencia por su API REST (llave de comercio,
 * idempotente por llave del comercio) y permite consultar una autorización
 * para conciliar. Nunca recibe ni guarda PAN ni CVV.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PayKlikApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayKlikApplication.class, args);
    }
}
