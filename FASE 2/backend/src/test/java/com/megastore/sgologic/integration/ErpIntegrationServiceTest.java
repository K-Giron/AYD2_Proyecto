package com.megastore.sgologic.integration;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpIntegrationServiceTest {
    private final Map<UUID, ErpSale> sales = new HashMap<>();
    private final Map<UUID, ErpBatch> batches = new HashMap<>();
    private final ErpSaleRepository saleRepository = mock(ErpSaleRepository.class);
    private final ErpBatchRepository batchRepository = mock(ErpBatchRepository.class);
    private final ErpGatewayPort gateway = mock(ErpGatewayPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-22T12:00:00Z"), ZoneOffset.UTC);
    private final ErpIntegrationService service = new ErpIntegrationService(saleRepository, batchRepository,
            gateway, Runnable::run, new ImmediateTaskScheduler(), clock, ErpExportFormat.CSV);

    @BeforeEach
    void setUpRepositories() {
        when(saleRepository.save(any())).thenAnswer(invocation -> {
            ErpSale sale = invocation.getArgument(0);
            sales.put(sale.getId(), sale);
            return sale;
        });
        when(saleRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<ErpSale> input = invocation.getArgument(0);
            input.forEach(sale -> sales.put(sale.getId(), sale));
            return input;
        });
        when(saleRepository.findByOrderId(any())).thenAnswer(invocation -> sales.values().stream()
                .filter(sale -> sale.getOrderId().equals(invocation.getArgument(0))).findFirst());
        when(saleRepository.findByStatusOrderByConfirmedAtAsc(any())).thenAnswer(invocation -> sales.values().stream()
                .filter(sale -> sale.getStatus() == invocation.getArgument(0))
                .sorted(java.util.Comparator.comparing(ErpSale::getConfirmedAt)).toList());
        when(saleRepository.findByBatchId(any())).thenAnswer(invocation -> sales.values().stream()
                .filter(sale -> invocation.getArgument(0).equals(sale.getBatchId())).toList());
        when(batchRepository.save(any())).thenAnswer(invocation -> {
            ErpBatch batch = invocation.getArgument(0);
            batches.put(batch.getId(), batch);
            return batch;
        });
        when(batchRepository.findById(any())).thenAnswer(
                invocation -> Optional.ofNullable(batches.get(invocation.getArgument(0))));
    }

    @Test
    void exportsConfirmedSalesAsCsvAndMarksThemDelivered() {
        when(gateway.deliver(any(), any(), any())).thenReturn(
                new ErpGatewayPort.DeliveryResult(true, "ERP-ACK-1", null));
        ErpSale sale = registerSale(UUID.randomUUID());

        ErpBatch batch = service.runBatch(ErpExportFormat.CSV);

        assertThat(batch.getStatus()).isEqualTo(ErpBatchStatus.DELIVERED);
        assertThat(batch.getSaleCount()).isEqualTo(1);
        assertThat(batch.getPayload()).startsWith(
                "recordType,saleId,channel,pointOfSale,reference,confirmedAt,netTotal,taxTotal,grandTotal,currency");
        assertThat(batch.getPayload()).contains("SALE," + sale.getOrderId(), "WEB,POS-ONLINE", "100.00,12.00,112.00,GTQ");
        // Registro de control: cantidad y sumas del lote, para que el ERP valide el archivo completo.
        assertThat(batch.getPayload()).endsWith("TRAILER,1,,,,,100.00,12.00,112.00,GTQ");
        assertThat(sale.getStatus()).isEqualTo(ErpSaleStatus.DELIVERED);
    }

    @Test
    void emptyWindowIsRecordedWithoutSendingAnEmptyFile() {
        ErpBatch batch = service.runBatch(null);

        assertThat(batch.getStatus()).isEqualTo(ErpBatchStatus.EMPTY);
        assertThat(batch.getPayload()).isNull();
        verify(gateway, never()).deliver(any(), any(), any());
    }

    @Test
    void transientErpFailureRetriesThreeTimesAndMovesBatchToDeadLetter() {
        when(gateway.deliver(any(), any(), any()))
                .thenThrow(new ErpGatewayPort.ErpTimeoutException("timeout"));
        registerSale(UUID.randomUUID());

        ErpBatch batch = service.runBatch(ErpExportFormat.XML);

        assertThat(batch.getStatus()).isEqualTo(ErpBatchStatus.DEAD_LETTER);
        assertThat(batch.getAttempts()).isEqualTo(4);
        verify(gateway, times(4)).deliver(any(), any(), any());
    }

    @Test
    void deadLetterBatchCanBeRetriedManuallyOnceTheErpIsBack() {
        when(gateway.deliver(any(), any(), any()))
                .thenThrow(new ErpGatewayPort.ErpUnavailableException("ERP fuera de linea"))
                .thenThrow(new ErpGatewayPort.ErpUnavailableException("ERP fuera de linea"))
                .thenThrow(new ErpGatewayPort.ErpUnavailableException("ERP fuera de linea"))
                .thenThrow(new ErpGatewayPort.ErpUnavailableException("ERP fuera de linea"))
                .thenReturn(new ErpGatewayPort.DeliveryResult(true, "ERP-FILE:ventas.csv", null));
        ErpSale sale = registerSale(UUID.randomUUID());
        ErpBatch batch = service.runBatch(ErpExportFormat.CSV);
        assertThat(batch.getStatus()).isEqualTo(ErpBatchStatus.DEAD_LETTER);

        ErpBatch retried = service.retryBatch(batch.getId());

        assertThat(retried.getStatus()).isEqualTo(ErpBatchStatus.DELIVERED);
        assertThat(retried.getAttempts()).isEqualTo(1);
        assertThat(sale.getStatus()).isEqualTo(ErpSaleStatus.DELIVERED);
    }

    @Test
    void scheduledWindowResumesBatchesLeftPendingByARestart() {
        when(gateway.deliver(any(), any(), any())).thenReturn(
                new ErpGatewayPort.DeliveryResult(true, "ERP-ACK-2", null));
        ErpBatch orphan = ErpBatch.create(ErpExportFormat.CSV, "recordType\n", 1, clock.instant().minusSeconds(600));
        batches.put(orphan.getId(), orphan);
        when(batchRepository.findByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(java.util.List.of(orphan));

        service.runScheduledBatch();

        assertThat(orphan.getStatus()).isEqualTo(ErpBatchStatus.DELIVERED);
    }

    @Test
    void registrationIsIdempotentByOrderAndRejectsInconsistentTotals() {
        UUID orderId = UUID.randomUUID();
        ErpSale first = registerSale(orderId);
        ErpSale repeated = service.registerConfirmedSale(orderId, BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ONE, "USD", clock.instant());

        assertThat(repeated).isSameAs(first);
        assertThatThrownBy(() -> service.registerConfirmedSale(UUID.randomUUID(), BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.TEN, "GTQ", clock.instant()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neto mas impuesto");
    }

    private ErpSale registerSale(UUID orderId) {
        return service.registerConfirmedSale(orderId, new BigDecimal("100.00"), new BigDecimal("12.00"),
                new BigDecimal("112.00"), "gtq", clock.instant());
    }
}
