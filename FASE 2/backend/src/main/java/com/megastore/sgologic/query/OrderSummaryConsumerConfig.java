package com.megastore.sgologic.query;

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
    static final String SHIPMENT_QUEUE = "sgo.query.shipment-status.v1";
    static final String INCIDENT_QUEUE = "sgo.query.incident-registered.v1";
    static final String PAYMENT_QUEUE = "sgo.query.payment-status.v1";
    /** Deben coincidir con los tipos que publica {@code PaymentAttemptWriter} (payments). */
    static final java.util.Set<String> PAYMENT_EVENTS = java.util.Set.of("PaymentApproved", "PaymentRejected", "PaymentIndeterminate");

    /** Debe coincidir exactamente con {@code ShipmentService.SHIPMENT_STATUS_CHANGED} (logistics). */
    static final String SHIPMENT_STATUS_CHANGED_EVENT = "SHIPMENT_STATUS_CHANGED";
    /** Debe coincidir exactamente con {@code IncidentService.INCIDENT_REGISTERED} (incidents). */
    static final String INCIDENT_REGISTERED_EVENT = "INCIDENT_REGISTERED";

    @Bean
    Queue orderSummaryQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding orderSummaryBinding(Queue orderSummaryQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(orderSummaryQueue)
                .to(businessEventsExchange)
                .with("ORDER_STATUS_CHANGED");
    }

    @Bean
    Queue querySummaryShipmentQueue() {
        return QueueBuilder.durable(SHIPMENT_QUEUE).build();
    }

    @Bean
    Binding querySummaryShipmentBinding(Queue querySummaryShipmentQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(querySummaryShipmentQueue)
                .to(businessEventsExchange)
                .with(SHIPMENT_STATUS_CHANGED_EVENT);
    }

    @Bean
    Queue querySummaryIncidentQueue() {
        return QueueBuilder.durable(INCIDENT_QUEUE).build();
    }

    @Bean
    Binding querySummaryIncidentBinding(Queue querySummaryIncidentQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(querySummaryIncidentQueue)
                .to(businessEventsExchange)
                .with(INCIDENT_REGISTERED_EVENT);
    }

    @Bean
    Queue querySummaryPaymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    Binding querySummaryPaymentApprovedBinding(Queue querySummaryPaymentQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(querySummaryPaymentQueue).to(businessEventsExchange).with("PaymentApproved");
    }

    @Bean
    Binding querySummaryPaymentRejectedBinding(Queue querySummaryPaymentQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(querySummaryPaymentQueue).to(businessEventsExchange).with("PaymentRejected");
    }

    @Bean
    Binding querySummaryPaymentIndeterminateBinding(Queue querySummaryPaymentQueue, TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(querySummaryPaymentQueue).to(businessEventsExchange).with("PaymentIndeterminate");
    }
}
