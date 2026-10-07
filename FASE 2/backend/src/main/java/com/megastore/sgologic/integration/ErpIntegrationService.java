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
        return registerConfirmedSale(orderId, netTotal, taxTotal, grandTotal, currency, confirmedAt,
                "WEB", "POS-ONLINE", orderId.toString());
    }

    /**
     * Venta confirmada de cualquier canal para el siguiente lote: WEB (orden
     * del monolito) o STORE (factura de tienda física recibida por el
     * almacén central). Idempotente por {@code orderId}.
     */
    public ErpSale registerConfirmedSale(UUID orderId, BigDecimal netTotal, BigDecimal taxTotal,
                                         BigDecimal grandTotal, String currency, Instant confirmedAt,
                                         String channel, String pointOfSale, String sourceReference) {
        Optional<ErpSale> existing = saleRepository.findByOrderId(orderId);
        if (existing.isPresent()) { return existing.get(); }
        if (netTotal.add(taxTotal).compareTo(grandTotal) != 0) {
            throw new IllegalArgumentException("El total ERP debe coincidir con neto mas impuesto.");
        }
        ErpSale sale = ErpSale.register(orderId, netTotal, taxTotal, grandTotal,
                currency.trim().toUpperCase(), confirmedAt, channel, pointOfSale, sourceReference, clock.instant());
        return saleRepository.save(sale);
    }

    /** Divide un total con IVA incluido (12 %) en neto e impuesto, de modo que neto + IVA = total. */
    public static BigDecimal[] splitIva(BigDecimal grandTotal) {
        BigDecimal net = grandTotal.divide(new BigDecimal("1.12"), 2, java.math.RoundingMode.HALF_UP);
        return new BigDecimal[]{net, grandTotal.subtract(net)};
    }

    /** Ventas confirmadas en un rango, para el informe de ventas conciliado. */
    @Transactional(readOnly = true)
    public List<ErpSaleView> salesConfirmedBetween(Instant from, Instant to) {
        return saleRepository.findByConfirmedAtGreaterThanEqualAndConfirmedAtLessThanOrderByConfirmedAtAsc(from, to)
                .stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ErpSaleView> saleFor(UUID orderId) {
        return saleRepository.findByOrderId(orderId).map(this::view);
    }

    @Transactional(readOnly = true)
    public List<ErpBatch> recentBatches() {
        return batchRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private ErpSaleView view(ErpSale sale) {
        String batchStatus = sale.getBatchId() == null ? null
                : batchRepository.findById(sale.getBatchId()).map(batch -> batch.getStatus().name()).orElse(null);
        return new ErpSaleView(sale.getOrderId(), sale.getChannel(), sale.getPointOfSale(), sale.getSourceReference(),
                sale.getNetTotal(), sale.getTaxTotal(), sale.getGrandTotal(), sale.getCurrency(), sale.getConfirmedAt(),
                sale.getStatus().name(), sale.getBatchId(), batchStatus);
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
        resumeOrphanedBatches();
        runBatch(defaultFormat);
    }

    /**
     * Los reintentos se programan en memoria; si el backend se reinició a la
     * mitad, el lote quedaría pendiente para siempre. Cada ventana retoma los
     * lotes pendientes que llevan más de un minuto sin moverse.
     */
    void resumeOrphanedBatches() {
        batchRepository.findByStatusInAndUpdatedAtBefore(
                        List.of(ErpBatchStatus.PENDING, ErpBatchStatus.PENDING_RETRY), clock.instant().minusSeconds(60))
                .forEach(batch -> dispatchExecutor.execute(() -> deliverBatch(batch.getId())));
    }

    /** Reintento manual de un lote que agotó sus reintentos automáticos (DEAD_LETTER). */
    @Transactional
    public ErpBatch retryBatch(UUID batchId) {
        ErpBatch batch = getBatch(batchId);
        if (batch.getStatus() != ErpBatchStatus.DEAD_LETTER) {
            return batch;
        }
        batch.reopenForRetry(clock.instant());
        batch = batchRepository.save(batch);
        dispatchAfterCommit(batch.getId());
        return batch;
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

    /**
     * Resumen de ventas conciliado para el AS/400: una línea por venta (canal,
     * punto de venta, referencia, neto, IVA, total) y un registro de control
     * con la cantidad y las sumas, para que el ERP verifique el archivo
     * completo antes de procesarlo.
     */
    private String generatePayload(List<ErpSale> sales, ErpExportFormat format) {
        BigDecimal net = sales.stream().map(ErpSale::getNetTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = sales.stream().map(ErpSale::getTaxTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = sales.stream().map(ErpSale::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (format == ErpExportFormat.CSV) {
            return "recordType,saleId,channel,pointOfSale,reference,confirmedAt,netTotal,taxTotal,grandTotal,currency\n"
                    + sales.stream().map(sale -> String.join(",", "SALE",
                            sale.getOrderId().toString(), sale.getChannel(), nullToEmpty(sale.getPointOfSale()),
                            nullToEmpty(sale.getSourceReference()), sale.getConfirmedAt().toString(),
                            sale.getNetTotal().toPlainString(), sale.getTaxTotal().toPlainString(),
                            sale.getGrandTotal().toPlainString(), sale.getCurrency()))
                    .collect(Collectors.joining("\n"))
                    + "\nTRAILER," + sales.size() + ",,,,," + net.toPlainString() + "," + tax.toPlainString() + ","
                    + total.toPlainString() + "," + sales.getFirst().getCurrency();
        }
        return sales.stream().map(sale -> "<Sale id=\"" + sale.getOrderId() + "\" channel=\"" + sale.getChannel()
                        + "\" pointOfSale=\"" + xml(sale.getPointOfSale()) + "\" reference=\"" + xml(sale.getSourceReference())
                        + "\" confirmedAt=\"" + sale.getConfirmedAt() + "\"><Net>"
                        + sale.getNetTotal().toPlainString() + "</Net><Tax>"
                        + sale.getTaxTotal().toPlainString() + "</Tax><Total>"
                        + sale.getGrandTotal().toPlainString() + "</Total><Currency>"
                        + sale.getCurrency() + "</Currency></Sale>")
                .collect(Collectors.joining("", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Sales>",
                        "<Control count=\"" + sales.size() + "\" net=\"" + net.toPlainString() + "\" tax=\""
                                + tax.toPlainString() + "\" total=\"" + total.toPlainString() + "\"/></Sales>"));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private static String xml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }
}
