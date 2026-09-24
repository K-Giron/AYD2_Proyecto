package com.megastore.sgologic.payments.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A1 / EaC-12: la referencia segura rechaza PAN (incluso con separadores) y
 * variantes de CVV/CVC antes de crear un intento.
 */
class PaymentRequestTest {

    @Test
    void acceptsASafeReference() {
        PaymentRequest request = request("tok_test_123");

        assertThat(request.paymentReference()).isEqualTo("tok_test_123");
    }

    @Test
    void rejectsPanWithAndWithoutSeparators() {
        assertThatThrownBy(() -> request("4111111111111111"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("PAN");
        assertThatThrownBy(() -> request("4111-1111-1111-1111"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> request("4111 1111 1111 1111"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> request("4111.1111.1111.1111"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> request("4111-1111-1111-1111-1234"))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    @Test
    void rejectsCvvAndCvcVariants() {
        for (String reference : new String[]{"cvv=123", "CVV2=123", "cvc=123", "cvc2=123", "securityCode=123"}) {
            assertThatThrownBy(() -> request(reference))
                    .as("referencia insegura: %s", reference)
                    .isInstanceOf(InvalidPaymentRequestException.class);
        }
    }

    @Test
    void rejectsOversizedReferenceOrIdempotencyKey() {
        assertThatThrownBy(() -> request("tok_" + "x".repeat(130)))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("128");
        assertThatThrownBy(() -> new PaymentRequest(UUID.randomUUID(), new BigDecimal("10.00"), "GTQ",
                "MOBILE", "CARD", "tok_test_1", UUID.randomUUID(), "k".repeat(129)))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("128");
    }

    @Test
    void rejectsMissingOrInvalidFields() {
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        assertThatThrownBy(() -> new PaymentRequest(null, new BigDecimal("10.00"), "GTQ", "MOBILE", "CARD",
                "tok_test_1", correlationId, "idem"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> new PaymentRequest(orderId, BigDecimal.ZERO, "GTQ", "MOBILE", "CARD",
                "tok_test_1", correlationId, "idem"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ$", "MOBILE", "CARD",
                "tok_test_1", correlationId, "idem"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ", " ", "CARD",
                "tok_test_1", correlationId, "idem"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ", "MOBILE", " ",
                "tok_test_1", correlationId, "idem"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ", "MOBILE", "CARD",
                "tok_test_1", null, "idem"))
                .isInstanceOf(InvalidPaymentRequestException.class);
        assertThatThrownBy(() -> new PaymentRequest(orderId, new BigDecimal("10.00"), "GTQ", "MOBILE", "CARD",
                "tok_test_1", correlationId, " "))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    private PaymentRequest request(String reference) {
        return new PaymentRequest(UUID.randomUUID(), new BigDecimal("150.00"), "GTQ", "MOBILE", "CARD",
                reference, UUID.randomUUID(), "idem-" + UUID.randomUUID());
    }
}
