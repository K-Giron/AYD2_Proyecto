package com.megastore.sgologic.inventorysync;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class InventorySyncConsumerConfig {

    static final String QUEUE = "sgo.inventorysync.order-status.v1";

    @Bean
    Queue inventorySyncOrderStatusQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding inventorySyncOrderStatusBinding(Queue inventorySyncOrderStatusQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(inventorySyncOrderStatusQueue)
                .to(businessEventsExchange)
                .with("ORDER_STATUS_CHANGED");
    }
}
