package com.megastore.inventoryservice.pointofsale;

import com.megastore.inventoryservice.auditlog.AuditLogService;
import com.megastore.inventoryservice.warehouse.Warehouse;
import com.megastore.inventoryservice.warehouse.WarehouseNotFoundException;
import com.megastore.inventoryservice.warehouse.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PointOfSaleService {

    private final PointOfSaleRepository repository;
    private final WarehouseRepository warehouseRepository;
    private final AuditLogService auditLogService;

    public PointOfSaleService(PointOfSaleRepository repository, WarehouseRepository warehouseRepository,
                               AuditLogService auditLogService) {
        this.repository = repository;
        this.warehouseRepository = warehouseRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Cada punto de venta recibe su propio almacén al crearse (no comparte
     * el almacén de otro punto de venta) — su código deriva del código del
     * punto de venta, que ya es único, así que no puede chocar con otro.
     */
    @Transactional
    public PointOfSale create(String code, String name) {
        if (repository.existsByCode(code)) {
            throw new DuplicatePointOfSaleCodeException(code);
        }
        Warehouse warehouse = warehouseRepository.save(Warehouse.create("ALM-" + code, "Almacén " + name));
        PointOfSale pointOfSale = repository.save(PointOfSale.create(code, name, warehouse.getId()));
        auditLogService.record("PUNTO_VENTA_CREADO",
                "Se creó el punto de venta " + code + " (" + name + ") con su propio almacén " + warehouse.getCode() + ".",
                null, pointOfSale.getId());
        return pointOfSale;
    }

    @Transactional(readOnly = true)
    public List<PointOfSaleResponse> listAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    /** Usado por otros módulos (movimientos, envíos) para mostrar tienda + almacén en sus respuestas. */
    @Transactional(readOnly = true)
    public PointOfSaleResponse getResponseById(UUID pointOfSaleId) {
        PointOfSale pointOfSale = repository.findById(pointOfSaleId)
                .orElseThrow(() -> new PointOfSaleNotFoundException(pointOfSaleId));
        return toResponse(pointOfSale);
    }

    @Transactional(readOnly = true)
    public PointOfSaleResponse getResponseByCode(String code) {
        PointOfSale pointOfSale = repository.findByCode(code)
                .orElseThrow(() -> new PointOfSaleNotFoundException(code));
        return toResponse(pointOfSale);
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID pointOfSaleId) {
        return repository.existsById(pointOfSaleId);
    }

    private PointOfSaleResponse toResponse(PointOfSale pointOfSale) {
        Warehouse warehouse = warehouseRepository.findById(pointOfSale.getWarehouseId())
                .orElseThrow(() -> new WarehouseNotFoundException(pointOfSale.getWarehouseId()));
        return PointOfSaleResponse.from(pointOfSale, warehouse);
    }
}
