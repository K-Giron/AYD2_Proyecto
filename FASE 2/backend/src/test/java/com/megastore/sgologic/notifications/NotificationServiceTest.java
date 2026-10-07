package com.megastore.sgologic.notifications;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de NOT-01: la solicitud vuelve de inmediato en PENDING (no
 * bloqueante) y el despacho síncrono-en-prueba ejercita éxito y el
 * backoff 2/8/32 s con DLQ ante indisponibilidad sostenida del proveedor
 * (CDU-11, E1).
 */
class NotificationServiceTest {

    private final NotificationRequestRepository repository = mock(NotificationRequestRepository.class);
    private final NotificationDeadLetterRepository deadLetterRepository = mock(NotificationDeadLetterRepository.class);
    private final NotificationProviderPort provider = mock(NotificationProviderPort.class);
    private final Executor immediateExecutor = Runnable::run;
    private final TaskScheduler immediateScheduler = new ImmediateTaskScheduler();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-17T09:00:00Z"), ZoneOffset.UTC);

    private final NotificationService service = new NotificationService(repository, deadLetterRepository,
            provider, immediateExecutor, immediateScheduler, clock);

    @Test
    void requestNotificationReturnsPendingAndSendsOnSuccess() throws Exception {
        UUID sourceId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.of(capturedRequest(id));
        });

        NotificationRequest created = service.requestNotification(
                NotificationEventType.ORDER_CONFIRMED, NotificationChannel.EMAIL, "cliente@example.com",
                sourceId, UUID.randomUUID());

        assertThat(created.getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(provider, times(1)).send(eq(NotificationChannel.EMAIL), eq("cliente@example.com"),
                eq(NotificationEventType.ORDER_CONFIRMED), any());
    }

    @Test
    void exhaustsThreeRetriesThenSendsToDeadLetterQueue() throws Exception {
        NotificationRequest request = NotificationRequest.create(NotificationEventType.SHIPMENT_PREPARING,
                NotificationChannel.SMS, "+50212345678", UUID.randomUUID(), UUID.randomUUID(), clock.instant());
        UUID id = request.getId();
        when(repository.findById(any())).thenReturn(Optional.of(request));
        // save() debe devolver la entidad guardada, como lo hace Spring Data;
        // devolver null ocultaba el manejo real de la version de la entidad.
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new NotificationProviderUnavailableException("caído"))
                .when(provider).send(any(), any(), any(), any());

        service.dispatch(id);

        // 1 intento inicial + 3 reintentos (2/8/32 s) = 4 llamadas al proveedor.
        verify(provider, times(4)).send(any(), any(), any(), any());
        assertThat(request.getStatus()).isEqualTo(NotificationStatus.FAILED);
        ArgumentCaptor<NotificationDeadLetter> captor = ArgumentCaptor.forClass(NotificationDeadLetter.class);
        verify(deadLetterRepository).save(captor.capture());
        assertThat(captor.getValue().getAttempts()).isEqualTo(4);
        assertThat(captor.getValue().getNotificationId()).isEqualTo(id);
    }

    private NotificationRequest capturedRequest(UUID id) {
        NotificationRequest request = NotificationRequest.create(NotificationEventType.ORDER_CONFIRMED,
                NotificationChannel.EMAIL, "cliente@example.com", UUID.randomUUID(), UUID.randomUUID(), clock.instant());
        return request;
    }

    private static final class ImmediateTaskScheduler implements TaskScheduler {
        @Override
        public java.util.concurrent.ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            task.run();
            return null;
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> schedule(Runnable task, org.springframework.scheduling.Trigger trigger) {
            task.run();
            return null;
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, java.time.Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(Runnable task, java.time.Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, java.time.Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, java.time.Duration delay) {
            throw new UnsupportedOperationException();
        }
    }
}
