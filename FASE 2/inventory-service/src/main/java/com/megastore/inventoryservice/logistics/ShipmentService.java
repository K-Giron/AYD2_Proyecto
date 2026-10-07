package com.megastore.inventoryservice.logistics;

import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.pointofsale.PointOfSaleResponse;
import com.megastore.inventoryservice.pointofsale.PointOfSaleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository repository;
    private final PointOfSaleService pointOfSaleService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ShipmentService(ShipmentRepository repository, PointOfSaleService pointOfSaleService,
                            AuditLogService auditLogService, Clock clock) {
        this.repository = repository;
        this.pointOfSaleService = pointOfSaleService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * Llamado por {@code StockService} al registrar cada folio de salida,
     * dentro de la misma transacción que ya ejecutó una actualización
     * masiva ({@code @Modifying(clearAutomatically = true)} sobre
     * existencia) — se usa {@code saveAndFlush} porque, tras ese
     * {@code clear()}, un {@code save} sin forzar el flush no deja la fila
     * escrita al confirmar la transacción.
     */
    @Transactional
    public Shipment createForFolio(String folio, UUID movementId, UUID pointOfSaleId) {
        return repository.saveAndFlush(Shipment.createForFolio(folio, movementId, pointOfSaleId, clock.instant()));
    }

    @Transactional(readOnly = true)
    public PointOfSaleResponse resolvePointOfSale(UUID pointOfSaleId) {
        return pointOfSaleService.getResponseById(pointOfSaleId);
    }

    @Transactional
    public Shipment advanceStatus(String folio, ShipmentStatus next) {
        Shipment shipment = repository.findByFolio(folio).orElseThrow(() -> new ShipmentNotFoundException(folio));
        shipment.advanceTo(next, clock.instant());
        Shipment saved = repository.save(shipment);
        auditLogService.record("ENVIO_ACTUALIZADO", "El envío del folio " + folio + " pasó a " + next + ".",
                folio, shipment.getPointOfSaleId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Shipment getByFolio(String folio) {
        return repository.findByFolio(folio).orElseThrow(() -> new ShipmentNotFoundException(folio));
    }

    @Transactional(readOnly = true)
    public List<Shipment> listAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
