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
    static final String SHIPMENT_QUEUE = "sgo.notifications.shipment-status.v1";
    static final String INCIDENT_QUEUE = "sgo.notifications.incident-registered.v1";
    static final String CREDIT_QUEUE = "sgo.notifications.credit-result.v1";

    /** Debe coincidir exactamente con {@code ShipmentService.SHIPMENT_STATUS_CHANGED} (logistics). */
    static final String SHIPMENT_STATUS_CHANGED_EVENT = "SHIPMENT_STATUS_CHANGED";
    /** Debe coincidir exactamente con {@code IncidentService.INCIDENT_REGISTERED} (incidents). */
    static final String INCIDENT_REGISTERED_EVENT = "INCIDENT_REGISTERED";

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

    /** LOG-01/02 (Logistics) publica preparación, tránsito y entrega (CUN-11). */
    @Bean
    Queue notificationShipmentStatusQueue() {
        return QueueBuilder.durable(SHIPMENT_QUEUE).build();
    }

    @Bean
    Binding notificationShipmentStatusBinding(Queue notificationShipmentStatusQueue,
                                              TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(notificationShipmentStatusQueue)
                .to(businessEventsExchange)
                .with(SHIPMENT_STATUS_CHANGED_EVENT);
    }

    /** INC-01 (Incidents) publica el registro de una devolución/incidencia (CUN-11). */
    @Bean
    Queue notificationIncidentRegisteredQueue() {
        return QueueBuilder.durable(INCIDENT_QUEUE).build();
    }

    @Bean
    Binding notificationIncidentRegisteredBinding(Queue notificationIncidentRegisteredQueue,
                                                  TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(notificationIncidentRegisteredQueue)
                .to(businessEventsExchange)
                .with(INCIDENT_REGISTERED_EVENT);
    }

    @Bean
    Queue notificationCreditResultQueue() {
        return QueueBuilder.durable(CREDIT_QUEUE).build();
    }

    @Bean
    Binding notificationCreditGrantedBinding(Queue notificationCreditResultQueue,
                                               TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(notificationCreditResultQueue)
                .to(businessEventsExchange)
                .with("CREDIT_GRANTED");
    }

    @Bean
    Binding notificationCreditRejectedBinding(Queue notificationCreditResultQueue,
                                                TopicExchange businessEventsExchange) {
        return BindingBuilder.bind(notificationCreditResultQueue)
                .to(businessEventsExchange)
                .with("CREDIT_REJECTED");
    }
}
