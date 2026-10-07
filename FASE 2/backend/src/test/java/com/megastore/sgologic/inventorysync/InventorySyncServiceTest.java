package com.megastore.sgologic.inventorysync;

import com.megastore.sgologic.shared.testing.ImmediateTaskScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas sin infraestructura: el planificador ejecuta de forma
 * síncrona (sin esperar los 2/8/32 s reales), y el puerto hacia
 * inventory-service se sustituye por un doble.
 */
class InventorySyncServiceTest {

    private final InventoryStockPort port = mock(InventoryStockPort.class);
    private final InventoryBridgeDeadLetterRepository deadLetterRepository = mock(InventoryBridgeDeadLetterRepository.class);
    private final TaskScheduler retryScheduler = new ImmediateTaskScheduler();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneOffset.UTC);

    private final InventorySyncService service =
            new InventorySyncService(port, deadLetterRepository, retryScheduler,
                    mock(com.megastore.sgologic.onlinestore.OnlineSyncLog.class), clock);

    @Test
    void syncOrderRegistersSaleForEachLine() {
        UUID orderId = UUID.randomUUID();
        SoldLine shirt = new SoldLine(UUID.randomUUID(), "Camisa manga larga azul", 2);
        SoldLine pants = new SoldLine(UUID.randomUUID(), "Pantalón de lona negro", 1);
        when(port.registerSale(any(), any())).thenReturn("POS-VTA-000001");

        service.syncOrder(orderId, List.of(shirt, pants), null, 1);

        verify(port).registerSale(shirt, orderId);
        verify(port).registerSale(pants, orderId);
        verify(deadLetterRepository, never()).save(any());
    }

    @Test
    void syncOrderSendsUnmatchedArticleStraightToDeadLetterWithoutRetrying() {
        UUID orderId = UUID.randomUUID();
        SoldLine line = new SoldLine(UUID.randomUUID(), "Producto que no existe en el almacén", 1);
        when(port.registerSale(line, orderId)).thenThrow(new InventoryArticleNotFoundException(line.productName()));

        service.syncOrder(orderId, List.of(line), null, 1);

        ArgumentCaptor<InventoryBridgeDeadLetter> captor = ArgumentCaptor.forClass(InventoryBridgeDeadLetter.class);
        verify(deadLetterRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getProductId()).isEqualTo(line.productId());
        // No debe haber programado ningún reintento: es un problema de datos, no de red.
        verify(port, times(1)).registerSale(any(), any());
    }

    @Test
    void webSaleWithoutOnlineStockGoesToDeadLetterWithoutRetrying() {
        UUID orderId = UUID.randomUUID();
        SoldLine line = new SoldLine(UUID.randomUUID(), "Camisa manga larga azul", 3);
        when(port.registerSale(line, orderId)).thenThrow(new InventoryStoreSaleRejectedException(
                "El punto de venta POS-ONLINE no pudo registrar la venta"));

        service.syncOrder(orderId, List.of(line), null, 1);

        verify(deadLetterRepository).save(any());
        verify(port, times(1)).registerSale(any(), any());
    }

    @Test
    void syncOrderRetriesTransientFailureAndEventuallySucceeds() {
        UUID orderId = UUID.randomUUID();
        SoldLine line = new SoldLine(UUID.randomUUID(), "Camisa manga larga azul", 1);
        when(port.registerSale(line, orderId))
                .thenThrow(new InventoryServiceUnavailableException("timeout", null))
                .thenThrow(new InventoryServiceUnavailableException("timeout", null))
                .thenReturn("SAL-000002");

        service.syncOrder(orderId, List.of(line), null, 1);

        // ImmediateTaskScheduler ejecuta los reintentos en el acto: 1 intento inicial + 2 reintentos = 3 llamadas.
        verify(port, times(3)).registerSale(line, orderId);
        verify(deadLetterRepository, never()).save(any());
    }

    @Test
    void syncOrderSendsToDeadLetterAfterExhaustingAllRetries() {
        UUID orderId = UUID.randomUUID();
        SoldLine line = new SoldLine(UUID.randomUUID(), "Camisa manga larga azul", 1);
        when(port.registerSale(line, orderId))
                .thenThrow(new InventoryServiceUnavailableException("inventory-service caído", null));

        service.syncOrder(orderId, List.of(line), null, 1);

        // 1 intento inicial + 3 reintentos (2/8/32s) = 4 llamadas antes de rendirse.
        verify(port, times(4)).registerSale(line, orderId);
        ArgumentCaptor<InventoryBridgeDeadLetter> captor = ArgumentCaptor.forClass(InventoryBridgeDeadLetter.class);
        verify(deadLetterRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getAttempts()).isEqualTo(4);
    }

    @Test
    void triggerSyncDoesNothingForEmptyLines() {
        service.triggerSync(UUID.randomUUID(), List.of(), null);

        verify(port, never()).registerSale(any(), any());
    }

    @Test
    void storeSaleGoesToTheStorePointOfSaleInsteadOfTheOnlineOne() {
        UUID orderId = UUID.randomUUID();
        SoldLine line = new SoldLine(UUID.randomUUID(), "Camisa manga larga azul", 1);
        when(port.registerStoreSale(line, orderId, "POS-01")).thenReturn("POS-VTA-000001");

        service.syncOrder(orderId, List.of(line), "POS-01", 1);

        verify(port).registerStoreSale(line, orderId, "POS-01");
        verify(port, never()).registerSale(any(), any());
    }

    @Test
    void storeSaleRejectedForMissingStockGoesToDeadLetterWithoutRetrying() {
        UUID orderId = UUID.randomUUID();
        SoldLine line = new SoldLine(UUID.randomUUID(), "Camisa manga larga azul", 5);
        when(port.registerStoreSale(line, orderId, "POS-01"))
                .thenThrow(new InventoryStoreSaleRejectedException("sin existencia en POS-01"));

        service.syncOrder(orderId, List.of(line), "POS-01", 1);

        verify(port, times(1)).registerStoreSale(line, orderId, "POS-01");
        verify(deadLetterRepository).save(any());
    }
}
