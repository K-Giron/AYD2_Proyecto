package com.megastore.sgologic.payments;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * Envuelve el adaptador PayKlik con límite de tiempo y circuit breaker
 * (PAY-02, ADR-003): una latencia superior al umbral o una racha de fallas
 * se convierten en un intento indeterminado sin propagar la falla en
 * cascada ni llamar al proveedor cuando el circuito está abierto.
 */
@Component
class ResilientPayKlikClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientPayKlikClient.class);

    private final PayKlikGatewayPort delegate;
    private final PayKlikResilienceProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final ExecutorService callExecutor = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "payklik-call");
        thread.setDaemon(true);
        return thread;
    });

    ResilientPayKlikClient(PayKlikGatewayPort delegate, PayKlikResilienceProperties properties) {
        this.delegate = delegate;
        this.properties = properties;
        this.circuitBreaker = CircuitBreaker.of("payklik", CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getFailureRateThreshold())
                .slidingWindowSize(properties.getSlidingWindowSize())
                .minimumNumberOfCalls(properties.getMinimumNumberOfCalls())
                .waitDurationInOpenState(properties.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(properties.getPermittedNumberOfCallsInHalfOpenState())
                .build());
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(properties.getTimeoutMs()))
                .build());
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Circuit breaker PayKlik: {} -> {}",
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
    }

    PayKlikGatewayPort.AuthorizationResult authorize(PayKlikGatewayPort.AuthorizationRequest request) {
        Callable<PayKlikGatewayPort.AuthorizationResult> timed = TimeLimiter.decorateFutureSupplier(
                timeLimiter, () -> CompletableFuture.supplyAsync(() -> delegate.authorize(request), callExecutor));
        Callable<PayKlikGatewayPort.AuthorizationResult> guarded =
                CircuitBreaker.decorateCallable(circuitBreaker, timed);
        try {
            return guarded.call();
        } catch (CallNotPermittedException circuitOpen) {
            throw new PayKlikCircuitOpenException(
                    "El circuit breaker de PayKlik está abierto; no se llama al proveedor.");
        } catch (TimeoutException timeout) {
            throw new PayKlikGatewayPort.PayKlikTimeoutException(
                    "PayKlik superó el umbral de " + properties.getTimeoutMs() + " ms.");
        } catch (ExecutionException executionFailure) {
            Throwable cause = executionFailure.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new PayKlikGatewayPort.PayKlikUnavailableException(
                    "La llamada a PayKlik falló: " + (cause == null ? executionFailure.getMessage() : cause.getMessage()));
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception unexpected) {
            throw new PayKlikGatewayPort.PayKlikUnavailableException(
                    "No fue posible completar la llamada a PayKlik: " + unexpected.getMessage());
        }
    }

    String circuitState() {
        return circuitBreaker.getState().name();
    }

    @PreDestroy
    void shutdownExecutor() {
        callExecutor.shutdownNow();
    }
}
