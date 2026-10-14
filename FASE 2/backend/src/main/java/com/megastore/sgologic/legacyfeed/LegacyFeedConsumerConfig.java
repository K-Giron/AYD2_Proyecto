package com.megastore.sgologic.legacyfeed;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LegacyFeedConsumerConfig {

    static final String QUEUE = "sgo.legacyfeed.order-status.v1";
    static final String SHIPMENT_QUEUE = "sgo.legacyfeed.shipment-status.v1";

    @Bean
    Queue legacyFeedOrderStatusQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding legacyFeedOrderStatusBinding(Queue legacyFeedOrderStatusQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(legacyFeedOrderStatusQueue)
                .to(businessEventsExchange)
                .with("ORDER_STATUS_CHANGED");
    }

    @Bean
    Queue legacyFeedShipmentStatusQueue() {
        return QueueBuilder.durable(SHIPMENT_QUEUE).build();
    }

    @Bean
    Binding legacyFeedShipmentStatusBinding(Queue legacyFeedShipmentStatusQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(legacyFeedShipmentStatusQueue).to(businessEventsExchange).with("SHIPMENT_STATUS_CHANGED");
    }
}
