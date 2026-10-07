package com.logiexpress.carrier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * LogiExpress: el transportista con el que MegaStore tiene contrato. Genera
 * la guía de cada envío por su API REST (con llave de comercio), lleva el
 * rastreo y tiene su propio portal donde los mensajeros registran la
 * recolección y la entrega. SGO-Logic solo lo consume por REST.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LogiExpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogiExpressApplication.class, args);
    }
}
