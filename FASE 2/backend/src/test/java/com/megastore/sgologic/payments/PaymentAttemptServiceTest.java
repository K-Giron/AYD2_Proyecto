package com.megastore.sgologic.payments;

import com.megastore.sgologic.payments.api.PaymentRequest;
import com.megastore.sgologic.payments.api.PaymentResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** PAY-01/PAY-02 / CDU-03: resultado, idempotencia e intento indeterminado. */
class PaymentAttemptServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T15:00:00Z");

    private final PaymentAttemptRepository repository = mock(PaymentAttemptRepository.class);
    private final ResilientPayKlikClient payKlikClient = mock(ResilientPayKlikClient.class);
    private final PaymentAttemptWriter writer = mock(PaymentAttemptWriter.class);
    private final PaymentAttemptService service = new PaymentAttemptService(repository, payKlikClient, writer);

    @Test
    void approvedAttemptRegistersOrderChannelReferenceAndResult() {
        UUID orderId = UUID.randomUUID();
        PaymentAttempt attempt = attempt(orderId, "idem-1");
        when(repository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(writer.create(any(), any(), any(), any(), anyString(), any(), anyString())).thenReturn(attempt);
        when(payKlikClient.authorize(any())).thenReturn(new PayKlikGatewayPort.AuthorizationResult(
                UUID.randomUUID(), true, "PK-123", null, NOW));
        PaymentAttempt approved = attempt(orderId, "idem-1");
        approved.markApproved("PK-123", UUID.randomUUID(), NOW);
        when(writer.markApproved(any(), anyString(), any())).thenReturn(approved);

        PaymentResult result = service.requestPayment(request(orderId, "idem-1", "tok_test_123"));

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.providerReference()).isEqualTo("PK-123");
        assertThat(result.paymentAttemptId()).isEqualTo(approved.getId());
        verify(payKlikClient).authorize(any());
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingWithoutCallingProvider() {
        UUID orderId = UUID.randomUUID();
        PaymentAttempt existing = attempt(orderId, "idem-2");
        when(repository.findByIdempotencyKey("idem-2")).thenReturn(Optional.of(existing));

        PaymentResult result = service.requestPayment(request(orderId, "idem-2", "tok_test_456"));

        assertThat(result.paymentAttemptId()).isEqualTo(existing.getId());
        assertThat(result.status()).isEqualTo("PENDING");
        verify(payKlikClient, never()).authorize(any());
        verify(writer, never()).create(any(), any(), any(), any(), anyString(), any(), anyString());
    }

    @Test
    void rejectedResultIsPersistedWithProviderAttempt() {
        UUID orderId = UUID.randomUUID();
        PaymentAttempt attempt = attempt(orderId, "idem-3");
        UUID providerAttemptId = UUID.randomUUID();
        when(repository.findByIdempotencyKey("idem-3")).thenReturn(Optional.empty());
        when(writer.create(any(), any(), any(), any(), anyString(), any(), anyString())).thenReturn(attempt);
        when(payKlikClient.authorize(any())).thenReturn(new PayKlikGatewayPort.AuthorizationResult(
                providerAttemptId, false, null, "INSUFFICIENT_FUNDS", NOW));
        PaymentAttempt rejected = attempt(orderId, "idem-3");
        rejected.markRejected("INSUFFICIENT_FUNDS", null, providerAttemptId, NOW);
        when(writer.markRejected(any(), anyString(), any(), any())).thenReturn(rejected);

        PaymentResult result = service.requestPayment(request(orderId, "idem-3", "tok_test_789"));

        assertThat(result.status()).isEqualTo("REJECTED");
        verify(writer).markRejected(attempt.getId(), "INSUFFICIENT_FUNDS", null, providerAttemptId);
    }

    @Test
    void timeoutLeavesAttemptIndeterminateAndNeverApproved() {
        UUID orderId = UUID.randomUUID();
        PaymentAttempt attempt = attempt(orderId, "idem-4");
        when(repository.findByIdempotencyKey("idem-4")).thenReturn(Optional.empty());
        when(writer.create(any(), any(), any(), any(), anyString(), any(), anyString())).thenReturn(attempt);
        when(payKlikClient.authorize(any()))
                .thenThrow(new PayKlikGatewayPort.PayKlikTimeoutException("sin respuesta"));
        PaymentAttempt indeterminate = attempt(orderId, "idem-4");
        indeterminate.markIndeterminate("PAYKLIK_TIMEOUT", "sin respuesta", NOW);
        when(writer.markIndeterminate(any(), anyString(), anyString())).thenReturn(indeterminate);

        PaymentResult result = service.requestPayment(request(orderId, "idem-4", "tok_test_999"));

        assertThat(result.status()).isEqualTo("INDETERMINATE");
        verify(writer).markIndeterminate(attempt.getId(), "PAYKLIK_TIMEOUT", "sin respuesta");
        verify(writer, never()).markApproved(any(), anyString(), any());
    }

    @Test
    void openCircuitLeavesAttemptIndeterminateWithoutProviderCall() {
        UUID orderId = UUID.randomUUID();
        PaymentAttempt attempt = attempt(orderId, "idem-6");
        when(repository.findByIdempotencyKey("idem-6")).thenReturn(Optional.empty());
        when(writer.create(any(), any(), any(), any(), anyString(), any(), anyString())).thenReturn(attempt);
        when(payKlikClient.authorize(any()))
                .thenThrow(new PayKlikCircuitOpenException("circuito abierto"));
        PaymentAttempt indeterminate = attempt(orderId, "idem-6");
        indeterminate.markIndeterminate("PAYKLIK_CIRCUIT_OPEN", "circuito abierto", NOW);
        when(writer.markIndeterminate(any(), anyString(), anyString())).thenReturn(indeterminate);

        PaymentResult result = service.requestPayment(request(orderId, "idem-6", "tok_test_111"));

        assertThat(result.status()).isEqualTo("INDETERMINATE");
        verify(writer).markIndeterminate(attempt.getId(), "PAYKLIK_CIRCUIT_OPEN", "circuito abierto");
    }

    @Test
    void unknownChannelIsRejectedBeforeCallingProvider() {
        UUID orderId = UUID.randomUUID();
        when(repository.findByIdempotencyKey("idem-5")).thenReturn(Optional.empty());
        PaymentRequest invalid = new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ", "FAX",
                "CARD", "tok_test_1", UUID.randomUUID(), "idem-5");

        assertThatThrownBy(() -> service.requestPayment(invalid))
                .isInstanceOf(InvalidPaymentRequestException.class);
        verify(payKlikClient, never()).authorize(any());
    }

    private PaymentRequest request(UUID orderId, String idempotencyKey, String reference) {
        return new PaymentRequest(orderId, new BigDecimal("150.00"), "GTQ", "MOBILE", "CARD",
                reference, UUID.randomUUID(), idempotencyKey);
    }

    private PaymentAttempt attempt(UUID orderId, String idempotencyKey) {
        return PaymentAttempt.create(UUID.randomUUID(), orderId, PaymentChannel.MOBILE, PaymentMethod.CARD,
                new BigDecimal("150.00"), "GTQ", UUID.randomUUID(), idempotencyKey, NOW);
    }
}
