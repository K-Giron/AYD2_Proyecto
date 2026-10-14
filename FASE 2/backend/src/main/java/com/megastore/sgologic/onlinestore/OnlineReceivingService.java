package com.megastore.sgologic.onlinestore;

import com.megastore.sgologic.inventory.InventoryMovementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Recepción por folio de la tienda online: lo que el central envía a
 * POS-ONLINE queda en tránsito hasta que alguien confirma el folio; al
 * confirmar entra al inventario del monolito en la ubicación de la tienda
 * online (CD-WEB), y el producto aparece en el catálogo administrable, sin
 * publicar todavía.
 */
@Service
public class OnlineReceivingService {

    private static final Logger log = LoggerFactory.getLogger(OnlineReceivingService.class);

    private final CentralReceiptsClient central;
    private final OnlineReceiptRepository receiptRepository;
    private final CatalogProductRepository productRepository;
    private final InventoryMovementService inventoryMovementService;
    private final OnlineStoreProperties properties;
    private final OnlineSyncLog syncLog;
    private final Clock clock;
    private volatile boolean lastPullFailed;
    private volatile Instant lastContactAt;

    OnlineReceivingService(CentralReceiptsClient central, OnlineReceiptRepository receiptRepository,
                           CatalogProductRepository productRepository,
                           InventoryMovementService inventoryMovementService, OnlineStoreProperties properties,
                           OnlineSyncLog syncLog, Clock clock) {
        this.syncLog = syncLog;
        this.central = central;
        this.receiptRepository = receiptRepository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.properties = properties;
        this.clock = clock;
    }

    /** Baja envíos nuevos del central; cada folio POS-ENT se registra una sola vez, en tránsito. */
    @Transactional
    public synchronized int pullReceipts() {
        List<IncomingReceipt> incoming;
        try {
            incoming = central.receiptsSince(receiptRepository.findLatestOccurredAt().orElse(null));
        } catch (OnlineStoreException unavailable) {
            if (!lastPullFailed) {
                log.warn("Tienda online sin conexión con el almacén central: {}", unavailable.getMessage());
                syncLog.record(OnlineSyncLog.BAJADA, OnlineSyncLog.RECEPCION, null, false, unavailable.getMessage());
            }
            lastPullFailed = true;
            throw unavailable;
        }
        if (lastPullFailed) {
            syncLog.record(OnlineSyncLog.BAJADA, OnlineSyncLog.RECEPCION, null, true,
                    "Conexión con el almacén central restablecida.");
        }
        lastPullFailed = false;
        lastContactAt = clock.instant();
        int added = 0;
        for (IncomingReceipt receipt : incoming) {
            if (!receiptRepository.existsById(receipt.folio())) {
                OnlineReceipt saved = receiptRepository.saveAndFlush(OnlineReceipt.inTransit(receipt));
                added++;
                syncLog.record(OnlineSyncLog.BAJADA, OnlineSyncLog.RECEPCION, saved.getShipmentFolio(), true,
                        "En tránsito: " + saved.getQuantity() + " × " + saved.getSku() + " (" + saved.getName()
                                + ") enviado por " + ("CENTRAL".equals(saved.getOrigin()) ? "el almacén central" : saved.getOrigin())
                                + ". Suma al inventario al confirmar la recepción.");
            }
        }
        return added;
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> inTransit() {
        return receiptRepository.findByStatusOrderByOccurredAtAsc(OnlineReceipt.Status.EN_TRANSITO).stream()
                .map(OnlineReceipt::getShipmentFolio)
                .distinct()
                .map(folio -> ShipmentResponse.of(folio, receiptRepository.findByShipmentFolio(normalize(folio))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnlineReceipt> history() {
        return receiptRepository.findTop200ByOrderByOccurredAtDesc();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse find(String folio) {
        return ShipmentResponse.of(normalize(folio), receiptRepository.findByShipmentFolio(normalize(folio)));
    }

    /**
     * Confirma que llegó todo lo de un folio: cada línea pendiente entra al
     * inventario (INBOUND en CD-WEB, con su evento) y, si es la primera vez,
     * crea su ficha de catálogo sin publicar. Todo o nada; una segunda
     * confirmación del mismo folio no vuelve a ingresar nada.
     */
    @Transactional
    public ShipmentResponse confirm(String folio, String actor) {
        String normalized = normalize(folio);
        List<OnlineReceipt> lines = receiptRepository.findByShipmentFolio(normalized);
        if (lines.isEmpty()) {
            throw new OnlineStoreException(HttpStatus.NOT_FOUND, "FOLIO_NOT_FOUND",
                    "El folio " + folio + " no tiene mercadería enviada a la tienda online (o el central aún no lo ha entregado).");
        }
        List<OnlineReceipt> pending = lines.stream()
                .filter(line -> line.getStatus() == OnlineReceipt.Status.EN_TRANSITO)
                .toList();
        if (pending.isEmpty()) {
            throw new OnlineStoreException(HttpStatus.CONFLICT, "FOLIO_ALREADY_RECEIVED",
                    "El folio " + folio + " ya fue recibido por " + lines.getFirst().getReceivedBy() + ".");
        }
        Instant now = clock.instant();
        List<OnlineReceipt> received = new ArrayList<>();
        for (OnlineReceipt line : pending) {
            if (receiptRepository.markReceived(line.getFolio(), actor, now) == 0) {
                continue;
            }
            inventoryMovementService.receiveAtLocation(line.getProductId(), properties.getLocationCode(),
                    line.getQuantity(), "Recepción tienda online, folio " + line.getShipmentFolio()
                            + " (" + line.getFolio() + ")", UUID.randomUUID());
            if (!productRepository.existsById(line.getProductId())) {
                productRepository.saveAndFlush(CatalogProduct.fromReceipt(line, now));
            }
            received.add(line);
        }
        if (received.isEmpty()) {
            throw new OnlineStoreException(HttpStatus.CONFLICT, "FOLIO_ALREADY_RECEIVED",
                    "El folio " + folio + " acaba de ser recibido por otra persona.");
        }
        syncLog.record(OnlineSyncLog.BAJADA, OnlineSyncLog.RECEPCION, received.getFirst().getShipmentFolio(), true,
                "Recibido por " + actor + ": " + received.stream().mapToInt(OnlineReceipt::getQuantity).sum()
                        + " unidad(es) (" + String.join(", ", received.stream()
                        .map(line -> line.getQuantity() + " × " + line.getSku()).toList())
                        + "). Ya está en el inventario de la tienda online (CD-WEB).");
        return ShipmentResponse.of(normalized, receiptRepository.findByShipmentFolio(normalized));
    }

    public boolean isCentralReachable() {
        return lastContactAt != null && !lastPullFailed;
    }

    public Instant getLastContactAt() {
        return lastContactAt;
    }

    private static String normalize(String folio) {
        return folio == null ? "" : folio.trim().toUpperCase(Locale.ROOT);
    }
}
