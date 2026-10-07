package com.logiexpress.carrier.guides;

import com.logiexpress.carrier.shared.CarrierException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GuideService {

    private final GuideRepository guides;
    private final GuideEventRepository events;
    private final Clock clock;

    GuideService(GuideRepository guides, GuideEventRepository events, Clock clock) {
        this.guides = guides;
        this.events = events;
        this.clock = clock;
    }

    public record IssueCommand(String merchant, String merchantReference, String orderReference, String origin,
                               String destination, String pickupPoint) {
    }

    public record Issued(GuideResponse guide, boolean created) {
    }

    /** Emite la guía; si el comercio repite la misma referencia, devuelve la que ya existe. */
    @Transactional
    public Issued issue(IssueCommand command) {
        var existing = guides.findByMerchantAndMerchantReference(command.merchant(), command.merchantReference());
        if (existing.isPresent()) {
            return new Issued(view(existing.get()), false);
        }
        Instant now = clock.instant();
        String trackingNumber = "LGX-%08d".formatted(guides.nextTrackingSequence());
        Guide guide = guides.saveAndFlush(Guide.issue(trackingNumber, command.merchant(), command.merchantReference(),
                command.orderReference(), command.origin(), command.destination(), command.pickupPoint(), now));
        events.save(GuideEvent.of(guide.getId(), GuideStatus.CREADA,
                "Guía emitida para " + command.merchant() + (guide.getPickupPoint() == null ? "" : " · retiro en " + guide.getPickupPoint()),
                "API " + command.merchant(), now));
        return new Issued(view(guide), true);
    }

    @Transactional(readOnly = true)
    public GuideResponse find(String merchant, String trackingNumber) {
        Guide guide = guides.findByTrackingNumber(trackingNumber)
                .filter(found -> merchant == null || found.getMerchant().equals(merchant))
                .orElseThrow(() -> new CarrierException(HttpStatus.NOT_FOUND, "GUIDE_NOT_FOUND",
                        "No existe la guía " + trackingNumber + "."));
        return view(guide);
    }

    /** Rastreo para el comercio: guías que cambiaron después de {@code after}, en orden. */
    @Transactional(readOnly = true)
    public List<GuideResponse> updatedSince(String merchant, Instant after) {
        return views(guides.findTop200ByMerchantAndUpdatedAtAfterOrderByUpdatedAtAsc(merchant,
                after == null ? Instant.EPOCH : after));
    }

    @Transactional(readOnly = true)
    public List<GuideResponse> recent() {
        return views(guides.findTop300ByOrderByUpdatedAtDesc());
    }

    @Transactional
    public GuideResponse pickUp(String trackingNumber, String courier) {
        Guide guide = load(trackingNumber);
        guide.pickUp(courier, clock.instant());
        guides.save(guide);
        events.save(GuideEvent.of(guide.getId(), GuideStatus.EN_TRANSITO, "Recolectado en " + guide.getOrigin(),
                courier, clock.instant()));
        return view(guide);
    }

    @Transactional
    public GuideResponse deliver(String trackingNumber, String courier, String receivedBy) {
        Guide guide = load(trackingNumber);
        GuideStatus status = guide.deliver(courier, clock.instant());
        guides.save(guide);
        String note = status == GuideStatus.EN_PUNTO_DE_RETIRO
                ? "Dejado en la tienda " + guide.getPickupPoint() + " para que el cliente lo recoja"
                : "Entregado en " + guide.getDestination();
        if (receivedBy != null && !receivedBy.isBlank()) {
            note += " · recibió " + receivedBy.trim();
        }
        events.save(GuideEvent.of(guide.getId(), status, note, courier, clock.instant()));
        return view(guide);
    }

    private Guide load(String trackingNumber) {
        return guides.findByTrackingNumber(trackingNumber.trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new CarrierException(HttpStatus.NOT_FOUND, "GUIDE_NOT_FOUND",
                        "No existe la guía " + trackingNumber + "."));
    }

    private GuideResponse view(Guide guide) {
        return GuideResponse.of(guide, events.findByGuideIdInOrderByOccurredAtAsc(List.of(guide.getId())));
    }

    private List<GuideResponse> views(List<Guide> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<GuideEvent>> byGuide = events.findByGuideIdInOrderByOccurredAtAsc(
                list.stream().map(Guide::getId).toList()).stream().collect(Collectors.groupingBy(GuideEvent::getGuideId));
        return list.stream().map(guide -> GuideResponse.of(guide, byGuide.getOrDefault(guide.getId(), List.of()))).toList();
    }
}
