package com.megastore.sgologic.logistics.api;

import java.util.UUID;

/** Resultado estable del despacho sin exponer la entidad JPA de Logistics. */
public record DispatchResult(UUID shipmentId, UUID orderId, String status, String trackingNumber) {
}
