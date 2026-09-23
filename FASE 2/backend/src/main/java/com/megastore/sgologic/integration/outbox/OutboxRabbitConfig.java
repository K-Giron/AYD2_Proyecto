package com.megastore.sgologic.integration.outbox;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ del Outbox: exchange de eventos, exchange y cola de
 * mensajes fallidos. Los consumidores declaran sus propias colas durables
 * enlazadas a {@code sgo.events} con su routing key y dead-letter exchange.
 */
@Configuration
class OutboxRabbitConfig {

    @Bean
    TopicExchange outboxExchange(OutboxProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    DirectExchange outboxDeadLetterExchange(OutboxProperties properties) {
        return new DirectExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    Queue outboxDeadLetterQueue(OutboxProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    @Bean
    Binding outboxDeadLetterBinding(Queue outboxDeadLetterQueue, DirectExchange outboxDeadLetterExchange) {
        return BindingBuilder.bind(outboxDeadLetterQueue).to(outboxDeadLetterExchange).with("#");
    }

    /**
     * Fuerza la declaración de la topología al arrancar. Sin esto, la
     * conexión es perezosa y el exchange solo se declara cuando ocurre la
     * primera publicación, de modo que un consumidor podría no encontrar el
     * exchange si se conecta antes.
     */
    @Bean
    ApplicationRunner outboxTopologyInitializer(AmqpAdmin amqpAdmin) {
        return args -> {
            if (amqpAdmin instanceof RabbitAdmin rabbitAdmin) {
                rabbitAdmin.initialize();
            }
        };
    }
}
