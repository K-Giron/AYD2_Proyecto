package com.megastore.sgologic.integration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

/** Implementa POS-01 para 32 tiendas, con acuse, reintento y deduplicacion. */
@Service
public class PosIntegrationService {

    static final long[] RETRY_DELAYS_SECONDS = {2L, 8L, 32L};

    private final PosDeliveryRepository repository;
    private final PosGatewayPort gateway;
    private final Executor dispatchExecutor;
    private final TaskScheduler retryScheduler;
    private final Clock clock;

    public PosIntegrationService(PosDeliveryRepository repository,
                                 PosGatewayPort gateway,
                                 @Qualifier("integrationDispatchExecutor") Executor dispatchExecutor,
                                 @Qualifier("posRetryScheduler") TaskScheduler retryScheduler,
                                 Clock clock) {
        this.repository = repository;
        this.gateway = gateway;
        this.dispatchExecutor = dispatchExecutor;
        this.retryScheduler = retryScheduler;
        this.clock = clock;
    }

    public PosDelivery publish(UUID eventId, String eventType, UUID orderId, int storeNumber,
                               String payload, UUID correlationId) {
        Optional<PosDelivery> existing = repository.findByEventIdAndStoreNumber(eventId, storeNumber);
        if (existing.isPresent()) { return existing.get(); }
        PosDelivery delivery = repository.save(PosDelivery.create(eventId, eventType, orderId,
                storeNumber, payload, correlationId, clock.instant()));
        dispatchExecutor.execute(() -> deliver(delivery.getId()));
        return delivery;
    }

    void deliver(UUID deliveryId) {
        Optional<PosDelivery> maybeDelivery = repository.findById(deliveryId);
        if (maybeDelivery.isEmpty()) { return; }
        PosDelivery delivery = maybeDelivery.get();
        if (delivery.getStatus() == PosDeliveryStatus.ACKNOWLEDGED
                || delivery.getStatus() == PosDeliveryStatus.REJECTED
                || delivery.getStatus() == PosDeliveryStatus.DEAD_LETTER) {
            return;
        }
        delivery.markAttempt(clock.instant());
        // Reasignar la copia gestionada: conserva la version vigente para el
        // siguiente save y evita el bloqueo optimista del objeto desconectado.
        delivery = repository.save(delivery);
        try {
            PosGatewayPort.DeliveryResult result = gateway.deliver(delivery.getStoreNumber(),
                    delivery.getEventId(), delivery.getEventType(), delivery.getOrderId(),
                    delivery.getPayload(), delivery.getCorrelationId());
            if (result.accepted()) {
                delivery.acknowledge(result.acknowledgement(), clock.instant());
            } else {
                delivery.reject(result.rejectionReason(), clock.instant());
            }
            repository.save(delivery);
        } catch (PosGatewayPort.PosTimeoutException | PosGatewayPort.PosUnavailableException failure) {
            retryOrDeadLetter(delivery, failure.getMessage());
        }
    }

    private void retryOrDeadLetter(PosDelivery delivery, String reason) {
        int retryIndex = delivery.getAttempts() - 1;
        if (retryIndex < RETRY_DELAYS_SECONDS.length) {
            delivery.pendingRetry(reason, clock.instant());
            delivery = repository.save(delivery);
            UUID deliveryId = delivery.getId();
            retryScheduler.schedule(() -> deliver(deliveryId),
                    clock.instant().plusSeconds(RETRY_DELAYS_SECONDS[retryIndex]));
        } else {
            delivery.deadLetter(reason, clock.instant());
            repository.save(delivery);
        }
    }

    public PosDelivery getDelivery(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IntegrationResourceNotFoundException("la entrega POS", id));
    }

    public List<PosDelivery> getDeliveriesForEvent(UUID eventId) {
        return repository.findByEventIdOrderByStoreNumberAsc(eventId);
    }
}
