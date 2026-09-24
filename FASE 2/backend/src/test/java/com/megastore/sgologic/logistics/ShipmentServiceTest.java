package com.megastore.sgologic.logistics;

import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de LOG-01/LOG-02/LOG-03 sin infraestructura: los repositorios y el
 * planificador se sustituyen por dobles, y las tareas planificadas se
 * ejecutan de forma síncrona para no depender de temporizadores reales.
 * {@code shipmentRepository} se respalda en un mapa en memoria: el
 * {@code Shipment} que {@code createDispatch} crea internamente tiene su
 * propio UUID generado, así que un doble ingenuo con IDs fijados de
 * antemano no lo encontraría en el reintento asíncrono.
 */
class ShipmentServiceTest {

    private final Map<UUID, Shipment> shipmentsById = new HashMap<>();
    private final ShipmentRepository shipmentRepository = mock(ShipmentRepository.class);
    private final ShipmentTrackingEventRepository trackingEventRepository = mock(ShipmentTrackingEventRepository.class);
    private final ShipmentDeadLetterRepository deadLetterRepository = mock(ShipmentDeadLetterRepository.class);
    private final LogiExpressGatewayPort gateway = mock(LogiExpressGatewayPort.class);
    private final TaskScheduler retryScheduler = new ImmediateTaskScheduler();
    private final PublishEvent eventPublisher = mock(PublishEvent.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), ZoneOffset.UTC);

    private final ShipmentService service = new ShipmentService(shipmentRepository, trackingEventRepository,
            deadLetterRepository, gateway, retryScheduler, eventPublisher, clock);

    ShipmentServiceTest() {
        when(shipmentRepository.save(any())).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipmentsById.put(shipment.getId(), shipment);
            return shipment;
        });
        when(shipmentRepository.findById(any())).thenAnswer(
                invocation -> Optional.ofNullable(shipmentsById.get(invocation.getArgument(0))));
    }

    @Test
    void createDispatchForCustomerPickupDoesNotRequestExternalLabel() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());

        Shipment shipment = service.createDispatch(
                orderId, DispatchOrigin.CUSTOMER_PICKUP, "Tienda Zona 10", UUID.randomUUID(), "idem-1");

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.READY_FOR_PICKUP);
        verify(gateway, never()).requestLabel(any(), any(), any(), any());
    }

    @Test
    void createDispatchIsIdempotentForRepeatedKey() {
        Shipment existing = Shipment.assign(UUID.randomUUID(), DispatchOrigin.STORE, "Tienda Norte",
                UUID.randomUUID(), "idem-2", clock.instant());
        when(shipmentRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.of(existing));

        Shipment result = service.createDispatch(
                existing.getOrderId(), DispatchOrigin.STORE, "Tienda Norte", UUID.randomUUID(), "idem-2");

        assertThat(result).isSameAs(existing);
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createDispatchPublishesPreparingEventForNotifications() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByIdempotencyKey("idem-6")).thenReturn(Optional.empty());

        service.createDispatch(orderId, DispatchOrigin.CUSTOMER_PICKUP, "Tienda Zona 10", UUID.randomUUID(), "idem-6");

        ArgumentCaptor<com.megastore.sgologic.integration.outbox.OutboxEventRequest> captor =
                ArgumentCaptor.forClass(com.megastore.sgologic.integration.outbox.OutboxEventRequest.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(ShipmentService.SHIPMENT_STATUS_CHANGED);
        assertThat(captor.getValue().aggregateType()).isEqualTo("SHIPMENT");
    }

    @Test
    void recordTrackingEventPublishesInTransitAndDeliveredEvents() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByIdempotencyKey("idem-7")).thenReturn(Optional.empty());
        Shipment created = service.createDispatch(orderId, DispatchOrigin.CUSTOMER_PICKUP, "Tienda Zona 10",
                UUID.randomUUID(), "idem-7");

        service.recordTrackingEvent(created.getId(), ShipmentStatus.IN_TRANSIT, "LOGIEXPRESS");
        service.recordTrackingEvent(created.getId(), ShipmentStatus.DELIVERED, "LOGIEXPRESS");

        // 1 al crear (PREPARING) + 1 en tránsito + 1 entregado = 3 publicaciones.
        verify(eventPublisher, times(3)).publish(any());
    }

    @Test
    void getShipmentsForOrderDelegatesToRepositoryOrderedByMostRecent() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = Shipment.assign(orderId, DispatchOrigin.STORE, "Tienda Centro",
                UUID.randomUUID(), "idem-5", clock.instant());
        when(shipmentRepository.findByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(List.of(shipment));

        List<Shipment> result = service.getShipmentsForOrder(orderId);

        assertThat(result).containsExactly(shipment);
    }

    @Test
    void requestsLabelAndMarksIssuedOnSuccess() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(gateway.requestLabel(any(), any(), any(), any()))
                .thenReturn(new LogiExpressLabelResult("LGX-TEST1234"));

        Shipment created = service.createDispatch(orderId, DispatchOrigin.DISTRIBUTION_CENTER, "San Salvador",
                UUID.randomUUID(), "idem-3");
        Shipment finalState = shipmentsById.get(created.getId());

        assertThat(finalState.getStatus()).isEqualTo(ShipmentStatus.LABEL_ISSUED);
        assertThat(finalState.getTrackingNumber()).isEqualTo("LGX-TEST1234");
        verify(trackingEventRepository).save(any(ShipmentTrackingEvent.class));
    }

    @Test
    void exhaustsThreeRetriesThenSendsToDeadLetterQueue() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByIdempotencyKey("idem-4")).thenReturn(Optional.empty());
        when(gateway.requestLabel(any(), any(), any(), any()))
                .thenThrow(new LogiExpressTimeoutException("timeout"));

        Shipment created = service.createDispatch(orderId, DispatchOrigin.STORE, "Antigua Guatemala",
                UUID.randomUUID(), "idem-4");
        Shipment finalState = shipmentsById.get(created.getId());

        // 1 intento inicial + 3 reintentos (2/8/32 s) = 4 llamadas al gateway.
        verify(gateway, times(4)).requestLabel(any(), any(), any(), any());
        assertThat(finalState.getStatus()).isEqualTo(ShipmentStatus.LABEL_FAILED);
        ArgumentCaptor<ShipmentDeadLetter> captor = ArgumentCaptor.forClass(ShipmentDeadLetter.class);
        verify(deadLetterRepository).save(captor.capture());
        assertThat(captor.getValue().getAttempts()).isEqualTo(4);
    }

    /** Ejecuta cualquier tarea planificada de inmediato, en el mismo hilo, para pruebas deterministas. */
    private static final class ImmediateTaskScheduler implements TaskScheduler {
        @Override
        public java.util.concurrent.ScheduledFuture<?> schedule(Runnable task, java.time.Instant startTime) {
            task.run();
            return null;
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> schedule(Runnable task, org.springframework.scheduling.Trigger trigger) {
            task.run();
            return null;
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(Runnable task, java.time.Instant startTime, java.time.Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(Runnable task, java.time.Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, java.time.Instant startTime, java.time.Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, java.time.Duration delay) {
            throw new UnsupportedOperationException();
        }
    }
}
