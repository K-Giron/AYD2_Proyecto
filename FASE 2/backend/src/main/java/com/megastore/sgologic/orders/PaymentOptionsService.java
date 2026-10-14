package com.megastore.sgologic.orders;

import com.megastore.sgologic.credit.api.CreditEvaluationRequest;
import com.megastore.sgologic.credit.api.CreditEvaluationResult;
import com.megastore.sgologic.credit.api.CreditPort;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/** Consulta rápida y fail-safe: una caída de crédito nunca bloquea PayKlik. */
@Service
class PaymentOptionsService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOptionsService.class);
    private final OrdersService orders;
    private final CreditPort credit;
    private final Clock clock;
    private final long timeoutMs;
    private final TimeLimiter timeLimiter;
    private final ExecutorService evaluationExecutor;

    PaymentOptionsService(OrdersService orders, CreditPort credit, Clock clock,
                          PaymentOptionsProperties properties) {
        this.orders = orders;
        this.credit = credit;
        this.clock = clock;
        this.timeoutMs = properties.getCreditTimeoutMs();
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(timeoutMs))
                .cancelRunningFuture(true)
                .build());
        this.evaluationExecutor = Executors.newFixedThreadPool(
                Math.max(1, properties.getCallPoolSize()), task -> {
                    Thread thread = new Thread(task, "credit-options-evaluation");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    PaymentOptionsResponse options(UUID cartId, UUID authenticatedCustomerId, UUID correlationId) {
        Cart cart = orders.getCart(cartId);
        if (!cart.getCustomerId().equals(authenticatedCustomerId)) {
            throw new CartOwnershipException(cartId);
        }
        List<PaymentOptionResponse> options = new ArrayList<>();
        options.add(PaymentOptionResponse.payKlik());
        CreditEvaluationResult evaluation = null;
        try {
            CreditEvaluationRequest request = new CreditEvaluationRequest(
                    cart.getCustomerId(), cart.total(), "GTQ", correlationId);
            Callable<CreditEvaluationResult> timed = TimeLimiter.decorateFutureSupplier(
                    timeLimiter, () -> evaluationExecutor.submit(() -> credit.evaluate(request)));
            evaluation = timed.call();
            if (evaluation.eligible()) {
                options.add(new PaymentOptionResponse(PaymentRoute.OWN_CREDIT, true,
                        evaluation.approvedLimit(),
                        LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(evaluation.termDays())));
            }
        } catch (TimeoutException timeout) {
            log.warn("Elegibilidad superó {} ms para cartId={} correlationId={}; se conserva PayKlik",
                    timeoutMs, cartId, correlationId);
        } catch (ExecutionException executionFailure) {
            Throwable failure = executionFailure.getCause() == null
                    ? executionFailure : executionFailure.getCause();
            log.warn("Elegibilidad no disponible para cartId={} correlationId={}; se conserva PayKlik: {}",
                    cartId, correlationId, failure.toString());
        } catch (Exception failure) {
            log.warn("Elegibilidad no disponible para cartId={} correlationId={}; se conserva PayKlik: {}",
                    cartId, correlationId, failure.toString());
        }
        return new PaymentOptionsResponse(cartId, "GTQ", cart.total(),
                evaluation == null ? clock.instant() : evaluation.evaluatedAt(),
                evaluation == null ? null : evaluation.ruleVersion(), List.copyOf(options));
    }

    @PreDestroy
    void shutdownExecutor() {
        evaluationExecutor.shutdownNow();
    }
}
