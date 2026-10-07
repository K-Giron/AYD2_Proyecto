package com.megastore.sgologic.legacyfeed;

import com.megastore.sgologic.logistics.Shipment;
import com.megastore.sgologic.logistics.ShipmentService;
import com.megastore.sgologic.messaging.BusinessEventEnvelope;
import com.megastore.sgologic.orders.CustomerOrder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detalle JSON que recibe el POS de una tienda sobre una compra web (orden, artículos, destino, tienda de retiro). */
@Component
class PosOrderPayloads {

    private static final Pattern PICKUP_STORE = Pattern.compile("\\((POS-(\\d{2}))\\)");

    private final ShipmentService shipmentService;
    private final ObjectMapper objectMapper;

    PosOrderPayloads(ShipmentService shipmentService, ObjectMapper objectMapper) {
        this.shipmentService = shipmentService;
        this.objectMapper = objectMapper;
    }

    /** Tienda donde el cliente recoge, por ejemplo {@code POS-01}, si la compra es para retiro. */
    static Optional<String> pickupStore(String destination) {
        if (destination == null) {
            return Optional.empty();
        }
        Matcher matcher = PICKUP_STORE.matcher(destination);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** Número de tienda (1..32) de un código {@code POS-xx}. */
    static int storeNumber(String pickupStore) {
        return Integer.parseInt(pickupStore.substring(4));
    }

    String forOrder(CustomerOrder order, String status, BusinessEventEnvelope event) {
        List<Shipment> shipments = shipmentService.getShipmentsForOrder(order.getId());
        Shipment shipment = shipments.isEmpty() ? null : shipments.getFirst();
        String destination = shipment == null ? null : shipment.getDestination();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.getId().toString());
        body.put("status", status);
        body.put("channel", order.getChannel().name());
        body.put("total", order.getTotal());
        body.put("currency", "GTQ");
        body.put("items", order.getItems().stream().map(item -> Map.of(
                "productId", item.getProductId().toString(), "name", item.getProductName(),
                "quantity", item.getQuantity(), "unitPrice", item.getUnitPrice())).toList());
        body.put("destination", destination);
        body.put("origin", shipment == null ? null : shipment.getOriginCode());
        body.put("pickupStore", pickupStore(destination).orElse(null));
        body.put("trackingNumber", shipment == null ? null : shipment.getTrackingNumber());
        body.put("occurredAt", event.occurredAt() == null ? null : event.occurredAt().toString());
        return objectMapper.writeValueAsString(body);
    }
}
