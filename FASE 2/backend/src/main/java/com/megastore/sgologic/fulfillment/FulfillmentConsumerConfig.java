package com.megastore.sgologic.fulfillment;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FulfillmentConsumerConfig {

    static final String QUEUE = "sgo.fulfillment.shipment-status.v1";

    @Bean
    Queue fulfillmentShipmentStatusQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding fulfillmentShipmentStatusBinding(Queue fulfillmentShipmentStatusQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(fulfillmentShipmentStatusQueue).to(businessEventsExchange).with("SHIPMENT_STATUS_CHANGED");
    }
}
