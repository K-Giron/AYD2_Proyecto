package com.megastore.sgologic.fiscal;

import com.megastore.sgologic.shared.testing.ImmediateTaskScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FiscalServiceTest {
    private final Map<UUID, FiscalDocument> documents = new HashMap<>();
    private final Map<String, FiscalDocument> documentsByKey = new HashMap<>();
    private final FiscalDocumentRepository repository = mock(FiscalDocumentRepository.class);
    private final SatGatewayPort gateway = mock(SatGatewayPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-22T12:00:00Z"), ZoneOffset.UTC);
    private final FiscalService service = new FiscalService(repository, new FiscalXmlGenerator(), gateway,
            Runnable::run, new ImmediateTaskScheduler(), clock);

    @BeforeEach
    void setUpRepository() {
        when(repository.save(any())).thenAnswer(invocation -> {
            FiscalDocument document = invocation.getArgument(0);
            documents.put(document.getId(), document);
            documentsByKey.put(document.getIdempotencyKey(), document);
            return document;
        });
        when(repository.findById(any())).thenAnswer(
                invocation -> Optional.ofNullable(documents.get(invocation.getArgument(0))));
        when(repository.findByIdempotencyKey(any())).thenAnswer(
                invocation -> Optional.ofNullable(documentsByKey.get(invocation.getArgument(0))));
        when(repository.existsByOrderIdAndStatus(any(), any())).thenAnswer(invocation -> documents.values().stream()
                .anyMatch(document -> document.getOrderId().equals(invocation.getArgument(0))
                        && document.getStatus() == invocation.getArgument(1)));
    }

    @Test
    void approvesInvoiceAndProducesWellFormedEscapedXml() {
        when(gateway.submit(any())).thenReturn(
                new SatGatewayPort.SatResult(true, "SAT-AUTH-1", null, "Aprobado"));
        UUID orderId = UUID.randomUUID();

        FiscalDocument document = request(orderId, "fis-1", "Cliente & <Socio>");

        assertThat(document.getStatus()).isEqualTo(FiscalStatus.APPROVED);
        assertThat(document.getSatAuthorization()).isEqualTo("SAT-AUTH-1");
        assertThat(document.getSubmissionAttempts()).isEqualTo(1);
        assertThat(document.getCui()).startsWith("CUI-");
        assertThat(document.getSeries()).isEqualTo("SGO-2026");
        assertThat(document.getXmlPayload()).contains("Cliente &amp; &lt;Socio&gt;");
        assertThat(service.canConfirmOrder(orderId)).isTrue();
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalDocumentWithoutResubmitting() {
        when(gateway.submit(any())).thenReturn(
                new SatGatewayPort.SatResult(true, "SAT-AUTH-2", null, "Aprobado"));
        UUID orderId = UUID.randomUUID();

        FiscalDocument first = request(orderId, "same-key", "Cliente");
        FiscalDocument second = request(orderId, "same-key", "Otro nombre");

        assertThat(second).isSameAs(first);
        verify(gateway, times(1)).submit(any());
    }

    @Test
    void explicitSatRejectionDoesNotEnableOrderConfirmation() {
        when(gateway.submit(any())).thenReturn(
                new SatGatewayPort.SatResult(false, null, "INVALID_TAX_ID", "NIT invalido"));
        UUID orderId = UUID.randomUUID();

        FiscalDocument document = request(orderId, "fis-rejected", "Cliente");

        assertThat(document.getStatus()).isEqualTo(FiscalStatus.REJECTED);
        assertThat(document.getFailureCode()).isEqualTo("INVALID_TAX_ID");
        assertThat(service.canConfirmOrder(orderId)).isFalse();
    }

    @Test
    void transientSatFailureRetriesThreeTimesAndRemainsFiscalPending() {
        when(gateway.submit(any())).thenThrow(new SatGatewayPort.SatTimeoutException("timeout"));
        UUID orderId = UUID.randomUUID();

        FiscalDocument document = request(orderId, "fis-timeout", "Cliente");

        assertThat(document.getStatus()).isEqualTo(FiscalStatus.FISCAL_PENDING);
        assertThat(document.getSubmissionAttempts()).isEqualTo(4);
        assertThat(document.getFailureCode()).isEqualTo("SAT_RETRY_EXHAUSTED");
        assertThat(service.canConfirmOrder(orderId)).isFalse();
        verify(gateway, times(4)).submit(any());
    }

    @Test
    void creditNoteRequiresExistingOriginalDocument() {
        assertThatThrownBy(() -> service.requestDocument(UUID.randomUUID(), UUID.randomUUID(),
                FiscalDocumentType.CREDIT_NOTE, null, "CF", "Cliente", new BigDecimal("1.00"),
                "GTQ", UUID.randomUUID(), "credit-without-original"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documento original");

        assertThatThrownBy(() -> service.requestDocument(UUID.randomUUID(), UUID.randomUUID(),
                FiscalDocumentType.CREDIT_NOTE, UUID.randomUUID(), "CF", "Cliente", new BigDecimal("1.00"),
                "GTQ", UUID.randomUUID(), "credit-missing-original"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    void creditNoteReferencesApprovedDocumentFromSameOrder() {
        when(gateway.submit(any())).thenReturn(
                new SatGatewayPort.SatResult(true, "SAT-CREDIT-AUTH", null, "Aprobado"));
        UUID orderId = UUID.randomUUID();
        FiscalDocument original = request(orderId, "original-invoice", "Cliente");

        FiscalDocument creditNote = service.requestDocument(orderId, UUID.randomUUID(),
                FiscalDocumentType.CREDIT_NOTE, original.getId(), "CF", "Cliente",
                new BigDecimal("25.00"), "GTQ", UUID.randomUUID(), "credit-note");

        assertThat(creditNote.getStatus()).isEqualTo(FiscalStatus.APPROVED);
        assertThat(creditNote.getOriginalDocumentId()).isEqualTo(original.getId());
        assertThat(creditNote.getXmlPayload()).contains(
                "<OriginalDocumentId>" + original.getId() + "</OriginalDocumentId>");
    }

    private FiscalDocument request(UUID orderId, String key, String customerName) {
        return service.requestDocument(orderId, UUID.randomUUID(), FiscalDocumentType.INVOICE, null,
                "CF", customerName, new BigDecimal("112.00"), "gtq", UUID.randomUUID(), key);
    }
}
