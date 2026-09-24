package com.megastore.sgologic.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Implementa NOT-01 / CUN-11: crea la solicitud, la registra y despacha el
 * envío en un hilo independiente para que la disponibilidad del proveedor
 * nunca bloquee la operación principal (EaC-08). Reintenta con el mismo
 * backoff que LOG-03 (2/8/32 s) y, al agotar los intentos, registra la
 * notificación en una cola de fallidos local (DLQ del trabajador) mientras
 * el equipo ratifica ADR-008.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    /** Mismo backoff que LOG-03 (2, 8 y 32 segundos) para mantener consistencia entre los módulos de Kevin. */
    static final long[] RETRY_DELAYS_SECONDS = {2L, 8L, 32L};

    private final NotificationRequestRepository repository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final NotificationProviderPort provider;
    private final Executor dispatchExecutor;
    private final TaskScheduler retryScheduler;
    private final Clock clock;

    public NotificationService(NotificationRequestRepository repository,
                                NotificationDeadLetterRepository deadLetterRepository,
                                NotificationProviderPort provider,
                                @Qualifier("notificationsDispatchExecutor") Executor dispatchExecutor,
                                @Qualifier("notificationsRetryScheduler") TaskScheduler retryScheduler,
                                Clock clock) {
        this.repository = repository;
        this.deadLetterRepository = deadLetterRepository;
        this.provider = provider;
        this.dispatchExecutor = dispatchExecutor;
        this.retryScheduler = retryScheduler;
        this.clock = clock;
    }

    /**
     * Crea la solicitud desde el evento de negocio y devuelve de inmediato
     * en estado {@code PENDING}; el envío ocurre de forma asíncrona.
     */
    @Transactional
    public NotificationRequest requestNotification(NotificationEventType eventType, NotificationChannel channel,
                                                     String recipient, UUID sourceAggregateId, UUID correlationId) {
        NotificationRequest request = NotificationRequest.create(
                eventType, channel, recipient, sourceAggregateId, correlationId, clock.instant());
        repository.save(request);
        UUID id = request.getId();
        dispatchAfterCommit(id);
        return request;
    }

    private void dispatchAfterCommit(UUID notificationId) {
        Runnable task = () -> dispatchExecutor.execute(() -> dispatch(notificationId));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { task.run(); }
            });
        } else {
            task.run();
        }
    }

    void dispatch(UUID id) {
        repository.findById(id).ifPresentOrElse(found -> {
            // Reasignar la copia gestionada en cada save: conserva la version
            // vigente y evita el bloqueo optimista del objeto desconectado.
            NotificationRequest request = found;
            request.markAttempt(clock.instant());
            request = repository.save(request);
            try {
                provider.send(request.getChannel(), request.getRecipient(), request.getEventType());
                request.markSent(clock.instant());
                repository.save(request);
            } catch (NotificationProviderUnavailableException failure) {
                handleDispatchFailure(request, failure.getMessage());
            }
        }, () -> log.warn("Se intentó despachar una notificación inexistente: {}", id));
    }

    private void handleDispatchFailure(NotificationRequest request, String reason) {
        int attemptsSoFar = request.getAttempts();
        int nextRetryIndex = attemptsSoFar - 1;
        UUID id = request.getId();
        if (nextRetryIndex < RETRY_DELAYS_SECONDS.length) {
            long delaySeconds = RETRY_DELAYS_SECONDS[nextRetryIndex];
            log.warn("Proveedor de notificaciones no disponible (intento {}) para {}; reintentando en {} s.",
                    attemptsSoFar, id, delaySeconds);
            request.markPendingRetry(reason, clock.instant());
            repository.save(request);
            retryScheduler.schedule(() -> dispatch(id), Instant.now().plusSeconds(delaySeconds));
        } else {
            log.error("Notificación {} quedó fallida tras agotar los reintentos: {}", id, reason);
            request.markFailed(reason, clock.instant());
            repository.save(request);
            deadLetterRepository.save(NotificationDeadLetter.of(id, reason, attemptsSoFar, clock.instant()));
        }
    }

    public NotificationRequest getNotification(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    public List<NotificationRequest> getNotificationsFor(UUID sourceAggregateId) {
        return repository.findBySourceAggregateIdOrderByCreatedAtAsc(sourceAggregateId);
    }
}
