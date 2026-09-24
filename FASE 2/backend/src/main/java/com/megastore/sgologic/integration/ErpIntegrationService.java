package com.megastore.sgologic.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/** Implementa ERP-01/ERP-02 con archivos persistidos, reintento e idempotencia. */
@Service
public class ErpIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ErpIntegrationService.class);
    static final long[] RETRY_DELAYS_SECONDS = {2L, 8L, 32L};

    private final ErpSaleRepository saleRepository;
    private final ErpBatchRepository batchRepository;
    private final ErpGatewayPort gateway;
    private final Executor dispatchExecutor;
    private final TaskScheduler retryScheduler;
    private final Clock clock;
    private final ErpExportFormat defaultFormat;

    public ErpIntegrationService(ErpSaleRepository saleRepository,
                                 ErpBatchRepository batchRepository,
                                 ErpGatewayPort gateway,
                                 @Qualifier("integrationDispatchExecutor") Executor dispatchExecutor,
                                 @Qualifier("erpRetryScheduler") TaskScheduler retryScheduler,
                                 Clock clock,
                                 @Value("${sgo.integrations.erp.format:CSV}") ErpExportFormat defaultFormat) {
        this.saleRepository = saleRepository;
        this.batchRepository = batchRepository;
        this.gateway = gateway;
        this.dispatchExecutor = dispatchExecutor;
        this.retryScheduler = retryScheduler;
        this.clock = clock;
        this.defaultFormat = defaultFormat;
    }

    public ErpSale registerConfirmedSale(UUID orderId, BigDecimal netTotal, BigDecimal taxTotal,
                                         BigDecimal grandTotal, String currency, Instant confirmedAt) {
        Optional<ErpSale> existing = saleRepository.findByOrderId(orderId);
        if (existing.isPresent()) { return existing.get(); }
        if (netTotal.add(taxTotal).compareTo(grandTotal) != 0) {
            throw new IllegalArgumentException("El total ERP debe coincidir con neto mas impuesto.");
        }
        ErpSale sale = ErpSale.register(orderId, netTotal, taxTotal, grandTotal,
                currency.trim().toUpperCase(), confirmedAt, clock.instant());
        return saleRepository.save(sale);
    }

    @Transactional
    public ErpBatch runBatch(ErpExportFormat requestedFormat) {
        ErpExportFormat format = requestedFormat == null ? defaultFormat : requestedFormat;
        List<ErpSale> sales = saleRepository.findByStatusOrderByConfirmedAtAsc(ErpSaleStatus.PENDING);
        String payload = sales.isEmpty() ? null : generatePayload(sales, format);
        ErpBatch batch = batchRepository.save(ErpBatch.create(format, payload, sales.size(), clock.instant()));
        if (sales.isEmpty()) {
            log.info("Ventana ERP ejecutada sin ventas nuevas; no se genero archivo vacio.");
            return batch;
        }
        sales.forEach(sale -> sale.includeIn(batch.getId(), clock.instant()));
        saleRepository.saveAll(sales);
        dispatchAfterCommit(batch.getId());
        return batch;
    }

    private void dispatchAfterCommit(UUID batchId) {
        Runnable dispatch = () -> dispatchExecutor.execute(() -> deliverBatch(batchId));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { dispatch.run(); }
            });
        } else {
            dispatch.run();
        }
    }

    @Scheduled(fixedDelayString = "${sgo.integrations.erp.interval-ms:1800000}",
            initialDelayString = "${sgo.integrations.erp.initial-delay-ms:1800000}")
    public void runScheduledBatch() {
        runBatch(defaultFormat);
    }

    void deliverBatch(UUID batchId) {
        Optional<ErpBatch> maybeBatch = batchRepository.findById(batchId);
        if (maybeBatch.isEmpty()) { return; }
        ErpBatch batch = maybeBatch.get();
        if (batch.getStatus() == ErpBatchStatus.EMPTY || batch.getStatus() == ErpBatchStatus.DELIVERED
                || batch.getStatus() == ErpBatchStatus.REJECTED || batch.getStatus() == ErpBatchStatus.DEAD_LETTER) {
            return;
        }
        batch.markAttempt(clock.instant());
        // Reasignar la copia gestionada: conserva la version vigente para el
        // siguiente save y evita el bloqueo optimista del objeto desconectado.
        batch = batchRepository.save(batch);
        try {
            ErpGatewayPort.DeliveryResult result = gateway.deliver(batch.getId(), batch.getFormat(), batch.getPayload());
            if (!result.accepted()) {
                batch.markRejected(result.rejectionReason(), clock.instant());
                batchRepository.save(batch);
                return;
            }
            batch.markDelivered(result.acknowledgement(), clock.instant());
            batchRepository.save(batch);
            List<ErpSale> sales = saleRepository.findByBatchId(batchId);
            sales.forEach(sale -> sale.markDelivered(clock.instant()));
            saleRepository.saveAll(sales);
        } catch (ErpGatewayPort.ErpTimeoutException | ErpGatewayPort.ErpUnavailableException failure) {
            retryOrDeadLetter(batch, failure.getMessage());
        }
    }

    private void retryOrDeadLetter(ErpBatch batch, String reason) {
        int retryIndex = batch.getAttempts() - 1;
        if (retryIndex < RETRY_DELAYS_SECONDS.length) {
            batch.markPendingRetry(reason, clock.instant());
            batch = batchRepository.save(batch);
            UUID batchId = batch.getId();
            retryScheduler.schedule(() -> deliverBatch(batchId),
                    clock.instant().plusSeconds(RETRY_DELAYS_SECONDS[retryIndex]));
        } else {
            batch.markDeadLetter(reason, clock.instant());
            batchRepository.save(batch);
        }
    }

    public ErpBatch getBatch(UUID batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IntegrationResourceNotFoundException("el lote ERP", batchId));
    }

    private String generatePayload(List<ErpSale> sales, ErpExportFormat format) {
        if (format == ErpExportFormat.CSV) {
            return "orderId,confirmedAt,netTotal,taxTotal,grandTotal,currency\n"
                    + sales.stream().map(sale -> String.join(",",
                            sale.getOrderId().toString(), sale.getConfirmedAt().toString(),
                            sale.getNetTotal().toPlainString(), sale.getTaxTotal().toPlainString(),
                            sale.getGrandTotal().toPlainString(), sale.getCurrency()))
                    .collect(Collectors.joining("\n"));
        }
        return sales.stream().map(sale -> "<Sale orderId=\"" + sale.getOrderId()
                        + "\" confirmedAt=\"" + sale.getConfirmedAt() + "\"><Net>"
                        + sale.getNetTotal().toPlainString() + "</Net><Tax>"
                        + sale.getTaxTotal().toPlainString() + "</Tax><Total>"
                        + sale.getGrandTotal().toPlainString() + "</Total><Currency>"
                        + sale.getCurrency() + "</Currency></Sale>")
                .collect(Collectors.joining("", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Sales>", "</Sales>"));
    }
}
