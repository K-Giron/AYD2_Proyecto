package com.megastore.sgologic.query;

import com.megastore.sgologic.messaging.OutboxRabbitConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OrderSummaryConsumerConfig {

    static final String QUEUE = "sgo.query.order-status.v1";

    @Bean
    Queue orderSummaryQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding orderSummaryBinding(Queue orderSummaryQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(orderSummaryQueue)
                .to(businessEventsExchange)
                .with("order.order_status_changed");
    }
}
