package com.megastore.sgologic.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Único bean {@link Clock} de la aplicación. Spring Modulith's Moments
 * ({@code spring-modulith-starter-core}) requiere exactamente un bean
 * {@code Clock} sin calificar; declarar un {@code Clock} por módulo rompe
 * el arranque completo con {@code NoUniqueBeanDefinitionException}. Es
 * infraestructura técnica, no regla de negocio, por lo que vive en
 * {@code shared} igual que {@code CorrelationIdFilter}.
 */
@Configuration
class SharedClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
