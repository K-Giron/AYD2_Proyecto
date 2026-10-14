package com.megastore.sgologic.credit;

import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.messaging.InboxService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Compensación del crédito: cuando una orden se cancela, la obligación vigente
 * se anula y el saldo vuelve al cliente. Reacciona al evento que ya publica
 * Orders, de modo que ese módulo no conoce ni depende del crédito para esto.
 */
@Component
@ConditionalOnProperty(name = "sgo.messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
class CreditOrderCancelledConsumer {

    static final String CONSUMER_NAME = "credit-order-cancelled-v1";

    private final InboxService inbox;
    private final CreditLifecycleService lifecycle;

    CreditOrderCancelledConsumer(InboxService inbox, CreditLifecycleService lifecycle) {
        this.inbox = inbox;
        this.lifecycle = lifecycle;
    }

    @RabbitListener(queues = CreditConsumerConfig.ORDER_STATUS_QUEUE)
    public void consume(BusinessEventEnvelope event) {
        validate(event);
        if (!"CANCELLED".equals(String.valueOf(event.payload().get("newStatus")))) return;
        inbox.processOnce(CONSUMER_NAME, event, () -> process(event));
    }

    void process(BusinessEventEnvelope event) {
        UUID correlationId = event.correlationId() == null ? UUID.randomUUID() : event.correlationId();
        lifecycle.cancelForOrder(event.aggregateId(), correlationId);
    }

    private void validate(BusinessEventEnvelope event) {
        if (!"ORDER_STATUS_CHANGED".equals(event.eventType()) || !"ORDER".equals(event.aggregateType())
                || event.aggregateId() == null || event.payload() == null
                || event.payload().get("newStatus") == null) {
            throw new IllegalArgumentException("Evento incompatible con la compensación de crédito");
        }
    }
}
