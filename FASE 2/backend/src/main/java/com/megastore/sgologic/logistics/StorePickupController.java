package com.megastore.sgologic.logistics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * La tienda de retiro (su sistema, rol STORE) confirma que entregó al
 * cliente el pedido que LogiExpress le dejó. Con eso el despacho y la orden
 * quedan entregados y el cliente recibe su aviso.
 */
@RestController
@RequestMapping("/api/v1/store-pickups")
class StorePickupController {

    private final ShipmentService shipmentService;

    StorePickupController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    record HandoffRequest(@NotBlank @Pattern(regexp = "POS-\\d{2}") String store,
                          @NotBlank @Size(max = 120) String receivedBy) {
    }

    @PostMapping("/{orderId}/handoff")
    ShipmentResponse handOver(@PathVariable UUID orderId, @Valid @RequestBody HandoffRequest request) {
        return ShipmentResponse.from(shipmentService.handOverAtStore(orderId, request.store(), request.receivedBy().trim()));
    }
}
