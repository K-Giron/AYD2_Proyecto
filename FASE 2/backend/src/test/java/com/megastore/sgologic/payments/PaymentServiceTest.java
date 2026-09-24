package com.megastore.sgologic.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-23T19:00:00Z");
    private final PaymentAttemptRepository repository = mock(PaymentAttemptRepository.class);
    private final PayKlikGatewayPort gateway = mock(PayKlikGatewayPort.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PaymentCircuitBreaker circuitBreaker = new PaymentCircuitBreaker(3, 30, clock);
    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(repository, gateway, circuitBreaker, scheduler, clock);
        when(repository.save(any(PaymentAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAndFlush(any(PaymentAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void persistsAttemptAndApprovesTokenizedPayment() {
        PaymentProcessingRequest request = request("pay-1", "tok_safe_1");
        when(repository.findByIdempotencyKey("pay-1")).thenReturn(Optional.empty());
        when(gateway.authorize(any())).thenReturn(new PayKlikGatewayPort.AuthorizationResult(
                UUID.randomUUID(), true, "PK-ABC", null, NOW));

        PaymentProcessingResult result = service.process(request);

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.providerReference()).isEqualTo("PK-ABC");
        verify(repository).saveAndFlush(any(PaymentAttempt.class));
    }

    @Test
    void timeoutPreservesIndeterminateAttemptAndSchedulesReconciliationWithinSixtySeconds() {
        when(repository.findByIdempotencyKey("pay-timeout")).thenReturn(Optional.empty());
        when(gateway.authorize(any())).thenThrow(new PayKlikGatewayPort.PayKlikTimeoutException("timeout"));

        PaymentProcessingResult result = service.process(request("pay-timeout", "tok_safe_2"));

        assertThat(result.status()).isEqualTo(PaymentStatus.INDETERMINATE);
        verify(scheduler).schedule(any(Runnable.class),
                org.mockito.ArgumentMatchers.eq(NOW.plusSeconds(30)));
    }

    @Test
    void repeatedKeyReturnsPriorResultWithoutChargingAgain() {
        PaymentAttempt existing = PaymentAttempt.pending(request("same-key", "tok_safe_3"), NOW);
        existing.approve("PK-EXISTING", NOW);
        when(repository.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        PaymentProcessingResult repeated = service.process(request("same-key", "tok_safe_3"));

        assertThat(repeated.providerReference()).isEqualTo("PK-EXISTING");
        verify(gateway, never()).authorize(any());
    }

    @Test
    void rejectsPanBeforePersistence() {
        when(repository.findByIdempotencyKey("unsafe")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.process(request("unsafe", "4111111111111111")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PAN/CVV");
        verify(repository, never()).saveAndFlush(any());
    }

    private PaymentProcessingRequest request(String key, String reference) {
        return new PaymentProcessingRequest(UUID.randomUUID(), "WEB", PaymentMethod.CARD,
                new BigDecimal("249.00"), "GTQ", reference, UUID.randomUUID(), key);
    }
}
