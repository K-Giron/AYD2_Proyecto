package com.megastore.sgologic.notifications;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NotificationConsumerConfig {

    static final String QUEUE = "sgo.notifications.order-status.v1";

    @Bean
    Queue notificationOrderStatusQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding notificationOrderStatusBinding(Queue notificationOrderStatusQueue,
                                           TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(notificationOrderStatusQueue)
                .to(businessEventsExchange)
                .with("ORDER_STATUS_CHANGED");
    }
}
