package com.megastore.inventoryservice.posevents;

import com.megastore.inventoryservice.article.Article;
import com.megastore.inventoryservice.article.ArticleRepository;
import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Comunicación POS local (stock): cada cambio de existencia de un punto de
 * venta genera un evento para el sistema de ESE punto de venta. Se guarda
 * en la misma transacción del movimiento (bandeja de salida) y se entrega
 * después, en orden por punto de venta, con reintentos 2/8/32 s; al agotarse
 * queda FALLIDO hasta que alguien lo reenvía desde "Stock por tienda".
 */
@Service
public class PosStockEventService {

    private static final Logger log = LoggerFactory.getLogger(PosStockEventService.class);
    static final long[] RETRY_DELAYS_SECONDS = {2L, 8L, 32L};
    private static final List<String> DUE = List.of(PosStockEvent.PENDING, PosStockEvent.RETRY, PosStockEvent.NO_TERMINAL);

    private final PosStockEventRepository repository;
    private final ArticleRepository articles;
    private final PointOfSaleService pointsOfSale;
    private final PosTerminalProperties properties;
    private final PosTerminalClient client;
    private final AuditLogService auditLog;
    private final Clock clock;

    PosStockEventService(PosStockEventRepository repository, ArticleRepository articles, PointOfSaleService pointsOfSale,
                         PosTerminalProperties properties, PosTerminalClient client, AuditLogService auditLog, Clock clock) {
        this.repository = repository;
        this.articles = articles;
        this.pointsOfSale = pointsOfSale;
        this.properties = properties;
        this.client = client;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /** Llamado por {@code PosStockService} dentro de la transacción del movimiento. */
    @Transactional
    public void enqueue(UUID pointOfSaleId, UUID articleId, String movement, int delta, int quantityAfter,
                        String folio, String reference) {
        String posCode = pointsOfSale.getResponseById(pointOfSaleId).code();
        Article article = articles.findById(articleId).orElse(null);
        repository.save(PosStockEvent.of(pointOfSaleId, posCode, articleId,
                article == null ? "?" : article.getSku(), article == null ? "Artículo " + articleId : article.getName(),
                movement, delta, quantityAfter, folio, readable(reference), clock.instant()));
    }

    /** La venta de tienda llega con su llave técnica "TKT:POS-01:TKT-000001:<artículo>": al POS le basta el ticket. */
    private static String readable(String reference) {
        if (reference != null && reference.startsWith("TKT:")) {
            String[] parts = reference.split(":");
            return parts.length >= 3 ? parts[2] : reference;
        }
        return reference;
    }

    /** Entrega lo pendiente. Si un evento de una tienda falla, los siguientes de esa tienda esperan (orden). */
    @Transactional
    public int deliverDue() {
        Instant now = clock.instant();
        Set<String> blocked = new HashSet<>();
        int delivered = 0;
        for (PosStockEvent event : repository.findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(DUE, now)) {
            if (blocked.contains(event.getPosCode())) {
                continue;
            }
            PosTerminalProperties.Terminal terminal = properties.getTerminals().get(event.getPosCode());
            if (terminal == null || !terminal.connected()) {
                event.noTerminal(now);
                blocked.add(event.getPosCode());
                continue;
            }
            try {
                event.delivered(client.deliver(terminal, event), clock.instant());
                delivered++;
            } catch (PosTerminalClient.DeliveryException failure) {
                event.failed(failure.getMessage(), RETRY_DELAYS_SECONDS, clock.instant());
                blocked.add(event.getPosCode());
                if (PosStockEvent.FAILED.equals(event.getStatus())) {
                    log.warn("Evento de stock {} para {} agotó sus reintentos: {}", event.getId(), event.getPosCode(),
                            failure.getMessage());
                    auditLog.record("POS_EVENTO_FALLIDO", "El evento de stock " + event.getMovement() + " " + event.getFolio()
                            + " no llegó al POS " + event.getPosCode() + " tras " + event.getAttempts() + " intentos: "
                            + event.getLastError(), event.getFolio(), event.getPointOfSaleId());
                }
            }
        }
        return delivered;
    }

    @Transactional
    public PosStockEvent retry(UUID eventId) {
        PosStockEvent event = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el evento " + eventId));
        if (PosStockEvent.FAILED.equals(event.getStatus())) {
            event.reopen(clock.instant());
        }
        return event;
    }

    @Transactional(readOnly = true)
    public List<PosStockEvent> recent(String posCode) {
        return posCode == null || posCode.isBlank()
                ? repository.findTop300ByOrderByCreatedAtDesc()
                : repository.findTop300ByPosCodeOrderByCreatedAtDesc(posCode);
    }
}
