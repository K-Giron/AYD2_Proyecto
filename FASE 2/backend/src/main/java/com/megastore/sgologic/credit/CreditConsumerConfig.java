package com.megastore.sgologic.credit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CreditConsumerConfig {

    static final String ORDER_STATUS_QUEUE = "sgo.credit.order-status.v1";

    @Bean
    Queue creditOrderStatusQueue() {
        return QueueBuilder.durable(ORDER_STATUS_QUEUE).build();
    }

    /** Debe coincidir con el tipo que publica {@code OrdersService} en cada cambio de estado. */
    @Bean
    Binding creditOrderStatusBinding(Queue creditOrderStatusQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(creditOrderStatusQueue)
                .to(businessEventsExchange)
                .with("ORDER_STATUS_CHANGED");
    }
}
