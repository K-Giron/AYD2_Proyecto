package com.megastore.sgologic.orders;

import com.megastore.sgologic.payments.api.PaymentMethod;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RunOrderSagaRequestTest {

    @Test
    void payKlikRequiresMethodAndReference() {
        var request = new RunOrderSagaRequest(PaymentRoute.PAYKLIK, null, "GTQ", null,
                "CF", "Cliente", "Destino");

        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(request)).isNotEmpty();
    }

    @Test
    void ownCreditRejectsPayKlikToken() {
        var request = new RunOrderSagaRequest(PaymentRoute.OWN_CREDIT, PaymentMethod.CARD,
                "GTQ", "tok_should_not_travel", "CF", "Cliente", "Destino");

        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(request)).isNotEmpty();
    }

    @Test
    void ownCreditAcceptsNoProviderCredentials() {
        var request = new RunOrderSagaRequest(PaymentRoute.OWN_CREDIT, null, "GTQ", null,
                "CF", "Cliente", "Destino");

        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(request)).isEmpty();
    }
}
