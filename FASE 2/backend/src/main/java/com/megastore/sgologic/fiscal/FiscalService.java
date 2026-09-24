package com.megastore.sgologic.fiscal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

/** Implementa FIS-01/FIS-02 sin leer tablas privadas de Orders o Payments. */
@Service
public class FiscalService implements FiscalProcessingPort {

    private static final Logger log = LoggerFactory.getLogger(FiscalService.class);
    static final long[] RETRY_DELAYS_SECONDS = {2L, 8L, 32L};

    private final FiscalDocumentRepository repository;
    private final FiscalXmlGenerator xmlGenerator;
    private final SatGatewayPort satGateway;
    private final Executor submissionExecutor;
    private final TaskScheduler retryScheduler;
    private final Clock clock;

    public FiscalService(FiscalDocumentRepository repository,
                         FiscalXmlGenerator xmlGenerator,
                         SatGatewayPort satGateway,
                         @Qualifier("fiscalSubmissionExecutor") Executor submissionExecutor,
                         @Qualifier("fiscalRetryScheduler") TaskScheduler retryScheduler,
                         Clock clock) {
        this.repository = repository;
        this.xmlGenerator = xmlGenerator;
        this.satGateway = satGateway;
        this.submissionExecutor = submissionExecutor;
        this.retryScheduler = retryScheduler;
        this.clock = clock;
    }

    public FiscalDocument requestDocument(UUID orderId, UUID paymentAttemptId,
                                          FiscalDocumentType documentType, UUID originalDocumentId,
                                          String taxIdentifier, String customerName, BigDecimal total,
                                          String currency, UUID correlationId, String idempotencyKey) {
        Optional<FiscalDocument> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (documentType == FiscalDocumentType.CREDIT_NOTE && originalDocumentId == null) {
            throw new IllegalArgumentException("Una nota de credito debe referenciar el documento original.");
        }
        if (documentType == FiscalDocumentType.CREDIT_NOTE) {
            FiscalDocument original = repository.findById(originalDocumentId)
                    .orElseThrow(() -> new IllegalArgumentException("El documento fiscal original no existe."));
            if (original.getStatus() != FiscalStatus.APPROVED || !original.getOrderId().equals(orderId)) {
                throw new IllegalArgumentException(
                        "La nota de credito requiere un documento original aprobado de la misma orden.");
            }
        }

        UUID documentId = UUID.randomUUID();
        String cui = "CUI-" + documentId.toString().replace("-", "").substring(0, 12).toUpperCase();
        String series = "SGO-" + Year.now(clock).getValue();
        String normalizedCurrency = currency.trim().toUpperCase();
        String xml = xmlGenerator.generate(documentId, orderId, documentType, originalDocumentId,
                taxIdentifier.trim(), customerName.trim(), total, normalizedCurrency, cui, series);
        FiscalDocument document = FiscalDocument.create(documentId, orderId, paymentAttemptId, documentType,
                originalDocumentId, taxIdentifier.trim(), customerName.trim(), total, normalizedCurrency,
                cui, series, xml, correlationId, idempotencyKey, clock.instant());
        repository.save(document);
        submitAfterCommit(documentId);
        return document;
    }

    /** Evita que el hilo asíncrono consulte el documento antes del commit de la Saga. */
    private void submitAfterCommit(UUID documentId) {
        Runnable submission = () -> submissionExecutor.execute(() -> submitToSat(documentId));
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submission.run();
                }
            });
            return;
        }
        submission.run();
    }

    void submitToSat(UUID documentId) {
        Optional<FiscalDocument> maybeDocument = repository.findById(documentId);
        if (maybeDocument.isEmpty()) {
            log.warn("No se encontro el documento fiscal {} al intentar enviarlo a SAT.", documentId);
            return;
        }
        FiscalDocument document = maybeDocument.get();
        if (document.getStatus() != FiscalStatus.FISCAL_PENDING) {
            return;
        }
        document.markAttempt(clock.instant());
        document = repository.save(document);
        try {
            SatGatewayPort.SatResult result = satGateway.submit(new SatGatewayPort.SatSubmission(
                    document.getId(), document.getOrderId(), document.getCui(), document.getSeries(),
                    document.getXmlPayload(), document.getCorrelationId(), document.getIdempotencyKey()));
            if (result.approved()) {
                document.approve(result.authorization(), clock.instant());
            } else {
                document.reject(result.rejectionCode(), result.message(), clock.instant());
            }
            repository.save(document);
        } catch (SatGatewayPort.SatTimeoutException | SatGatewayPort.SatUnavailableException failure) {
            scheduleRetryOrKeepPending(document, failure.getMessage());
        }
    }

    private void scheduleRetryOrKeepPending(FiscalDocument document, String reason) {
        int nextRetryIndex = document.getSubmissionAttempts() - 1;
        document.remainPending("SAT_TRANSIENT_FAILURE", reason, clock.instant());
        FiscalDocument saved = repository.save(document);
        if (nextRetryIndex < RETRY_DELAYS_SECONDS.length) {
            long delay = RETRY_DELAYS_SECONDS[nextRetryIndex];
            retryScheduler.schedule(() -> submitToSat(saved.getId()), clock.instant().plusSeconds(delay));
        } else {
            saved.remainPending("SAT_RETRY_EXHAUSTED",
                    "SAT sigue sin confirmar; la orden debe permanecer en FISCAL_PENDING.", clock.instant());
            repository.save(saved);
        }
    }

    public FiscalDocument getDocument(UUID id) {
        return repository.findById(id).orElseThrow(() -> new FiscalDocumentNotFoundException(id));
    }

    public List<FiscalDocument> getDocumentsForOrder(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    public boolean canConfirmOrder(UUID orderId) {
        return repository.existsByOrderIdAndStatus(orderId, FiscalStatus.APPROVED);
    }

    @Override
    public FiscalProcessingResult requestInvoice(FiscalProcessingRequest request) {
        return toResult(requestDocument(request.orderId(), request.paymentAttemptId(),
                FiscalDocumentType.INVOICE, null, request.taxIdentifier(), request.customerName(),
                request.total(), request.currency(), request.correlationId(), request.idempotencyKey()));
    }

    @Override
    public Optional<FiscalProcessingResult> latestForOrder(UUID orderId) {
        return getDocumentsForOrder(orderId).stream().findFirst().map(this::toResult);
    }

    private FiscalProcessingResult toResult(FiscalDocument document) {
        return new FiscalProcessingResult(document.getId(), document.getOrderId(), document.getStatus(),
                document.getCui(), document.getSeries(), document.getSatAuthorization(),
                document.getFailureCode());
    }
}
