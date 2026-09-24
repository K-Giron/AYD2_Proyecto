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
 * nunca bloquee la operación principal (EaC-08). Un único reintento
 * asíncrono cubre el flujo de excepción E1 mientras el equipo ratifica
 * ADR-008; no reemplaza una política de reintentos definitiva.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final long RETRY_DELAY_SECONDS = 5L;

    private final NotificationRequestRepository repository;
    private final NotificationProviderPort provider;
    private final Executor dispatchExecutor;
    private final TaskScheduler retryScheduler;
    private final Clock clock;

    public NotificationService(NotificationRequestRepository repository,
                                NotificationProviderPort provider,
                                @Qualifier("notificationsDispatchExecutor") Executor dispatchExecutor,
                                @Qualifier("notificationsRetryScheduler") TaskScheduler retryScheduler,
                                Clock clock) {
        this.repository = repository;
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
        Runnable task = () -> dispatchExecutor.execute(() -> dispatch(notificationId, true));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { task.run(); }
            });
        } else {
            task.run();
        }
    }

    void dispatch(UUID id, boolean allowRetry) {
        repository.findById(id).ifPresentOrElse(request -> {
            request.markAttempt(clock.instant());
            request = repository.save(request);
            try {
                provider.send(request.getChannel(), request.getRecipient(), request.getEventType());
                request.markSent(clock.instant());
                repository.save(request);
            } catch (NotificationProviderUnavailableException failure) {
                if (allowRetry) {
                    log.warn("Proveedor de notificaciones no disponible para {}; reintentando en {} s.",
                            id, RETRY_DELAY_SECONDS);
                    request.markPendingRetry(failure.getMessage(), clock.instant());
                    repository.save(request);
                    retryScheduler.schedule(() -> dispatch(id, false),
                            Instant.now().plusSeconds(RETRY_DELAY_SECONDS));
                } else {
                    log.error("Notificación {} quedó fallida tras agotar el reintento: {}", id, failure.getMessage());
                    request.markFailed(failure.getMessage(), clock.instant());
                    repository.save(request);
                }
            }
        }, () -> log.warn("Se intentó despachar una notificación inexistente: {}", id));
    }

    public NotificationRequest getNotification(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
    }

    public List<NotificationRequest> getNotificationsFor(UUID sourceAggregateId) {
        return repository.findBySourceAggregateIdOrderByCreatedAtAsc(sourceAggregateId);
    }
}
