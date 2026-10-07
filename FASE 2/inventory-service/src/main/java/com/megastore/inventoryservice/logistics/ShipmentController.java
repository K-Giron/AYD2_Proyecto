package com.megastore.inventoryservice.logistics;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<ShipmentResponse> list() {
        return shipmentService.listAll().stream()
                .map(shipment -> ShipmentResponse.from(shipment, shipmentService.resolvePointOfSale(shipment.getPointOfSaleId())))
                .toList();
    }

    @GetMapping("/{folio}")
    public ShipmentResponse getByFolio(@PathVariable String folio) {
        Shipment shipment = shipmentService.getByFolio(folio);
        return ShipmentResponse.from(shipment, shipmentService.resolvePointOfSale(shipment.getPointOfSaleId()));
    }

    @PatchMapping("/{folio}/status")
    public ShipmentResponse advanceStatus(@PathVariable String folio, @Valid @RequestBody UpdateShipmentStatusRequest request) {
        Shipment shipment = shipmentService.advanceStatus(folio, request.status());
        return ShipmentResponse.from(shipment, shipmentService.resolvePointOfSale(shipment.getPointOfSaleId()));
    }
}
